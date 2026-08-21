package com.truckerload.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.model.WeekSummary
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.screens.assistant.AssistantResult
import com.truckerload.presentation.screens.assistant.JournalAssistantPreview
import com.truckerload.presentation.screens.assistant.JournalMutationWriter
import com.truckerload.presentation.screens.assistant.PendingAssistantMutation
import com.truckerload.voice.AppVoiceAction
import com.truckerload.voice.AppVoiceActions
import com.truckerload.voice.AppVoiceJournal
import com.truckerload.voice.VoiceAssistantLogger
import com.truckerload.voice.VoiceCommandBus
import com.truckerload.voice.VoiceFailReason
import com.truckerload.voice.VoicePeerMatch
import com.truckerload.voice.VoicePeerRef
import com.truckerload.voice.VoicePendingDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VoicePeerChoice(
    val id: String,
    val label: String,
)

sealed class VoicePrompt {
    data class ConfirmCall(val peer: VoicePeerChoice) : VoicePrompt()
    data class ConfirmMessage(val peer: VoicePeerChoice, val text: String) : VoicePrompt()
    data class PickPeer(val candidates: List<VoicePeerChoice>, val action: AppVoiceAction) : VoicePrompt()
    data class ConfirmJournal(val mutation: PendingAssistantMutation) : VoicePrompt()
    data class WeeklyGross(val summary: WeekSummary) : VoicePrompt()
    data class Failed(val reason: VoiceFailReason) : VoicePrompt()
}

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val profileRepository: ProfileRepository,
    private val journalPreview: JournalAssistantPreview,
    private val mutationWriter: JournalMutationWriter,
) : ViewModel() {

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    private val _prompt = MutableStateFlow<VoicePrompt?>(null)
    val prompt: StateFlow<VoicePrompt?> = _prompt.asStateFlow()

    private val _journalSaving = MutableStateFlow(false)
    val journalSaving: StateFlow<Boolean> = _journalSaving.asStateFlow()

    init {
        viewModelScope.launch {
            VoiceCommandBus.pending.collect { command ->
                if (command != null) handle(command)
            }
        }
    }

    fun onNavigated() {
        _navigateTo.value = null
    }

    fun dismissPrompt() {
        _prompt.value = null
        _journalSaving.value = false
        VoiceCommandBus.consume()
    }

    fun pickPeer(peer: VoicePeerChoice) {
        val prompt = _prompt.value as? VoicePrompt.PickPeer ?: return
        _prompt.value = null
        viewModelScope.launch { fulfillPeer(prompt.action, peer) }
    }

    fun confirmSensitive() {
        val prompt = _prompt.value ?: return
        viewModelScope.launch {
            when (prompt) {
                is VoicePrompt.ConfirmCall -> startCall(prompt.peer)
                is VoicePrompt.ConfirmMessage -> openChat(prompt.peer, prompt.text)
                is VoicePrompt.ConfirmJournal -> confirmJournal(prompt.mutation)
                else -> Unit
            }
            if (prompt !is VoicePrompt.ConfirmJournal) {
                _prompt.value = null
            }
        }
    }

    fun fixJournal() {
        val mutation = (_prompt.value as? VoicePrompt.ConfirmJournal)?.mutation ?: return
        _prompt.value = null
        _navigateTo.value = when (mutation) {
            is PendingAssistantMutation.DieselDraft -> Routes.ADD_DIESEL
            is PendingAssistantMutation.PaycheckDraft -> Routes.ADD_PAYCHECK
        }
    }

    private suspend fun confirmJournal(mutation: PendingAssistantMutation) {
        if (_journalSaving.value) return
        _journalSaving.value = true
        try {
            withContext(Dispatchers.IO) { mutationWriter.save(mutation) }
            VoiceAssistantLogger.logOutcome("assistant", "saved")
            _journalSaving.value = false
            _prompt.value = null
        } catch (_: Exception) {
            VoiceAssistantLogger.logOutcome("assistant", "save_failed")
            _journalSaving.value = false
            fail(null, VoiceFailReason.UNKNOWN)
        }
    }

    private suspend fun handle(command: AppVoiceAction) {
        VoiceCommandBus.consume()
        when (command) {
            is AppVoiceAction.OpenScreen -> {
                VoiceAssistantLogger.log(command, "navigate")
                _navigateTo.value = command.route
            }
            is AppVoiceAction.ChatWithFriend,
            is AppVoiceAction.MessageFriend,
            is AppVoiceAction.CallFriend,
            -> resolvePeer(command)
            is AppVoiceAction.AddDiesel,
            is AppVoiceAction.AddPaycheck,
            is AppVoiceAction.QueryWeeklyGross,
            -> handleJournal(command)
        }
    }

    private suspend fun handleJournal(command: AppVoiceAction) {
        val toolCall = AppVoiceJournal.toToolCall(command)
        if (toolCall == null) {
            val form = AppVoiceJournal.formRoute(command)
            if (form != null) {
                VoiceAssistantLogger.log(command, "open_form")
                _navigateTo.value = form
            } else {
                fail(command, VoiceFailReason.UNKNOWN)
            }
            return
        }
        when (val result = journalPreview.fromToolCall(toolCall)) {
            is AssistantResult.Confirm -> {
                _prompt.value = VoicePrompt.ConfirmJournal(result.mutation)
            }
            is AssistantResult.WeeklyGross -> {
                _prompt.value = VoicePrompt.WeeklyGross(result.summary)
            }
            is AssistantResult.Ambiguous -> fail(command, VoiceFailReason.UNKNOWN)
            is AssistantResult.Failed,
            is AssistantResult.Saved,
            -> fail(command, VoiceFailReason.UNKNOWN)
        }
    }

    private suspend fun resolvePeer(command: AppVoiceAction) {
        val query = when (command) {
            is AppVoiceAction.ChatWithFriend -> command.peerQuery
            is AppVoiceAction.MessageFriend -> command.peerQuery
            is AppVoiceAction.CallFriend -> command.peerQuery
            else -> return
        }
        val peers = chatRepository.watchPeers().first().map { it.toRef() }
        when (val match = AppVoiceActions.matchPeers(query, peers)) {
            VoicePeerMatch.None -> fail(command, VoiceFailReason.PEER_NOT_FOUND)
            is VoicePeerMatch.Unique -> fulfillPeer(command, match.peer.toChoice())
            is VoicePeerMatch.Ambiguous -> {
                VoiceAssistantLogger.log(command, "ambiguous", match.candidates.size.toString())
                _prompt.value = VoicePrompt.PickPeer(
                    candidates = match.candidates.map { it.toChoice() },
                    action = command,
                )
            }
        }
    }

    private suspend fun fulfillPeer(command: AppVoiceAction, peer: VoicePeerChoice) {
        when (command) {
            is AppVoiceAction.ChatWithFriend -> openChat(peer, draft = null)
            is AppVoiceAction.MessageFriend -> {
                _prompt.value = VoicePrompt.ConfirmMessage(peer, command.text)
                VoiceAssistantLogger.log(command, "confirm_message")
            }
            is AppVoiceAction.CallFriend -> {
                _prompt.value = VoicePrompt.ConfirmCall(peer)
                VoiceAssistantLogger.log(command, "confirm_call")
            }
            else -> Unit
        }
    }

    private suspend fun openChat(peer: VoicePeerChoice, draft: String?) {
        when (val result = chatRepository.createPrivateChatWithPeer(peer.id)) {
            is SocialResult.Success -> {
                if (!draft.isNullOrBlank()) VoicePendingDraft.chatText = draft
                VoiceAssistantLogger.logOutcome("chat", "ok")
                _navigateTo.value = Routes.socialChat(result.data)
            }
            is SocialResult.Error -> fail(null, VoiceFailReason.PEER_NOT_FOUND)
        }
    }

    private suspend fun startCall(peer: VoicePeerChoice) {
        val me = profileRepository.watchMyProfile().first().displayName.ifBlank { "Me" }
        val call = voiceRepository.startCall(peer.id, peer.label, me).getOrNull()
        if (call == null) {
            fail(null, VoiceFailReason.PEER_NOT_FOUND)
            return
        }
        VoiceAssistantLogger.logOutcome("call", "ok")
        _navigateTo.value = Routes.call(call.callId)
    }

    private fun fail(command: AppVoiceAction?, reason: VoiceFailReason) {
        command?.let { VoiceAssistantLogger.log(it, reason.name.lowercase()) }
            ?: VoiceAssistantLogger.logOutcome("voice", reason.name.lowercase())
        _prompt.value = VoicePrompt.Failed(reason)
        if (reason == VoiceFailReason.PEER_NOT_FOUND) {
            _navigateTo.value = Routes.COMMUNITY
        }
    }

    private fun SocialPeerProfile.toRef() = VoicePeerRef(id = id, displayName = displayName)

    private fun VoicePeerRef.toChoice() = VoicePeerChoice(id = id, label = displayName)
}
