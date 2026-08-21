package com.truckerload.presentation.screens.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.assistant.SpeechToText
import com.truckerload.data.assistant.SpeechToTextError
import com.truckerload.data.assistant.SpeechToTextListener
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.voice.VoiceAssistantLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    application: Application,
    private val speechToText: SpeechToText,
    private val dispatcher: GeminiFunctionDispatcher,
    private val mutationWriter: JournalMutationWriter,
    private val settingsDataStore: SettingsDataStore,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VoiceAssistantUiState())
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    fun onMicTapped() {
        when (_uiState.value.phase) {
            AssistantPhase.Listening -> {
                speechToText.stop()
                _uiState.update { it.copy(phase = AssistantPhase.Idle) }
            }
            AssistantPhase.Processing -> return
            else -> startListening()
        }
    }

    fun onMicPermissionGranted() {
        _uiState.update { it.copy(needsMicPermission = false, errorMessageRes = null) }
        startListening()
    }

    fun onMicPermissionDenied() {
        _uiState.update {
            it.copy(
                phase = AssistantPhase.Error,
                needsMicPermission = false,
                errorMessageRes = R.string.assistant_error_mic_permission,
            )
        }
    }

    fun dismissResult() {
        _uiState.update {
            it.copy(
                result = null,
                phase = AssistantPhase.Idle,
                errorMessageRes = null,
            )
        }
    }

    fun fixMutation() {
        _uiState.update {
            it.copy(
                result = null,
                phase = AssistantPhase.Idle,
                errorMessageRes = null,
            )
        }
        startListening()
    }

    fun confirmMutation() {
        val pending = (_uiState.value.result as? AssistantResult.Confirm)?.mutation ?: return
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    mutationWriter.save(pending)
                }
                VoiceAssistantLogger.logOutcome("assistant", "saved")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        phase = AssistantPhase.Ready,
                        result = AssistantResult.Saved(pending),
                    )
                }
            } catch (_: Exception) {
                VoiceAssistantLogger.logOutcome("assistant", "save_failed")
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        phase = AssistantPhase.Error,
                        errorMessageRes = R.string.common_save_failed,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        speechToText.destroy()
        super.onCleared()
    }

    private fun startListening() {
        _uiState.update {
            it.copy(
                phase = AssistantPhase.Listening,
                result = null,
                errorMessageRes = null,
                transcript = "",
            )
        }
        viewModelScope.launch {
            val language = settingsDataStore.language.first().tag
            speechToText.start(language, object : SpeechToTextListener {
                override fun onListening() {
                    _uiState.update { it.copy(phase = AssistantPhase.Listening) }
                }

                override fun onPartial(text: String) {
                    _uiState.update { it.copy(transcript = text) }
                }

                override fun onFinal(text: String) {
                    processTranscript(text)
                }

                override fun onError(error: SpeechToTextError) {
                    handleSpeechError(error)
                }
            })
        }
    }

    private fun processTranscript(text: String) {
        _uiState.update {
            it.copy(
                transcript = text,
                phase = AssistantPhase.Processing,
                errorMessageRes = null,
            )
        }
        viewModelScope.launch {
            val language = settingsDataStore.language.first().tag
            val result = withContext(Dispatchers.IO) {
                dispatcher.interpret(text, language)
            }
            val errorRes = when (result) {
                is AssistantResult.Failed -> when (result.kind) {
                    AssistantFailKind.NO_API_KEY -> R.string.assistant_error_no_key
                    AssistantFailKind.NETWORK -> R.string.assistant_error_network
                }
                else -> null
            }
            val phase = when (result) {
                is AssistantResult.Failed, AssistantResult.Ambiguous -> AssistantPhase.Error
                else -> AssistantPhase.Ready
            }
            _uiState.update {
                it.copy(
                    phase = phase,
                    result = result,
                    errorMessageRes = errorRes,
                )
            }
        }
    }

    private fun handleSpeechError(error: SpeechToTextError) {
        when (error) {
            SpeechToTextError.PERMISSION -> {
                _uiState.update {
                    it.copy(
                        phase = AssistantPhase.Idle,
                        needsMicPermission = true,
                        errorMessageRes = null,
                    )
                }
            }
            SpeechToTextError.EMPTY -> {
                _uiState.update {
                    it.copy(
                        phase = AssistantPhase.Error,
                        result = AssistantResult.Ambiguous,
                        errorMessageRes = null,
                    )
                }
            }
            SpeechToTextError.UNAVAILABLE -> {
                _uiState.update {
                    it.copy(
                        phase = AssistantPhase.Error,
                        errorMessageRes = R.string.assistant_error_speech_unavailable,
                    )
                }
            }
            SpeechToTextError.FAILED -> {
                _uiState.update {
                    it.copy(
                        phase = AssistantPhase.Error,
                        errorMessageRes = R.string.assistant_error_speech_failed,
                    )
                }
            }
        }
    }
}
