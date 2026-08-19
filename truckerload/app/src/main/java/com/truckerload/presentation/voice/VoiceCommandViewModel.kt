package com.truckerload.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.presentation.navigation.Routes
import com.truckerload.voice.AppVoiceAction
import com.truckerload.voice.AppVoiceActions
import com.truckerload.voice.VoiceAssistantLogger
import com.truckerload.voice.VoiceCommandBus
import com.truckerload.voice.VoiceFailReason
import com.truckerload.voice.VoicePeerMatch
import com.truckerload.voice.VoicePeerRef
import com.truckerload.voice.VoicePendingDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VoicePeerChoice(
    val id: String,
    val label: String,
)

sealed class VoicePrompt {
    data class ConfirmCall(val peer: VoicePeerChoice) : VoicePrompt()
    data class ConfirmMessage(val peer: VoicePeerChoice, val text: String) : VoicePrompt()
    data class PickPeer(val candidates: List<VoicePeerChoice>, val action: AppVoiceAction) : VoicePrompt()
    data class Failed(val reason: VoiceFailReason) : VoicePrompt()
}

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val voiceRepository: VoiceRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    private val _prompt = MutableStateFlow<VoicePrompt?>(null)
    val prompt: StateFlow<VoicePrompt?> = _prompt.asStateFlow()

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
                else -> Unit
            }
            _prompt.value = null
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
        }
    }

    private suspend fun resolvePeer(command: AppVoiceAction) {
        val query = when (command) {
            is AppVoiceAction.ChatWithFriend -> command.peerQuery
            is AppVoiceAction.MessageFriend -> command.peerQuery
            is AppVoiceAction.CallFriend -> command.peerQuery
            is AppVoiceAction.OpenScreen -> return
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
            is AppVoiceAction.OpenScreen -> Unit
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
