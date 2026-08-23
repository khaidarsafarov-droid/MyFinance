package com.truckerload.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.domain.model.WeekSummary
import com.truckerload.presentation.navigation.Routes
import com.truckerload.presentation.screens.assistant.AssistantResult
import com.truckerload.presentation.screens.assistant.JournalAssistantPreview
import com.truckerload.presentation.screens.assistant.JournalMutationWriter
import com.truckerload.presentation.screens.assistant.PendingAssistantMutation
import com.truckerload.voice.AppVoiceAction
import com.truckerload.voice.AppVoiceJournal
import com.truckerload.voice.VoiceAssistantLogger
import com.truckerload.voice.VoiceCommandBus
import com.truckerload.voice.VoiceFailReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class VoicePrompt {
    data class ConfirmJournal(val mutation: PendingAssistantMutation) : VoicePrompt()
    data class WeeklyGross(val summary: WeekSummary) : VoicePrompt()
    data class Failed(val reason: VoiceFailReason) : VoicePrompt()
}

@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
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

    fun confirmSensitive() {
        val prompt = _prompt.value ?: return
        viewModelScope.launch {
            when (prompt) {
                is VoicePrompt.ConfirmJournal -> confirmJournal(prompt.mutation)
                else -> _prompt.value = null
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

    private fun fail(command: AppVoiceAction?, reason: VoiceFailReason) {
        command?.let { VoiceAssistantLogger.log(it, reason.name.lowercase()) }
            ?: VoiceAssistantLogger.logOutcome("voice", reason.name.lowercase())
        _prompt.value = VoicePrompt.Failed(reason)
    }
}
