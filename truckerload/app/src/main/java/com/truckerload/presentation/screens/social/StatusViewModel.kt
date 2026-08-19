package com.truckerload.presentation.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.repository.social.StatusRepository
import com.truckerload.domain.social.SocialResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusUiState(
    val statuses: List<com.truckerload.domain.social.DriverStatusPost> = emptyList(),
    val inputText: String = "",
    val isPosting: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {
    private val _input = MutableStateFlow("")
    private val _isRecordingVoice = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatusUiState> =
        combine(
            statusRepository.watchFriendStatuses(),
            _input,
            _isRecordingVoice,
            _errorMessage,
        ) { statuses, input, recording, error ->
            StatusUiState(
                statuses = statuses,
                inputText = input,
                isRecordingVoice = recording,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())

    init {
        viewModelScope.launch {
            socialSyncCoordinator.ensureInitialized()
            while (isActive) {
                delay(4_000)
                runCatching { socialSyncCoordinator.pullRemote() }
            }
        }
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun postTextStatus(displayName: String) {
        val text = _input.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            statusRepository.createTextStatus(text, displayName)
            _input.value = ""
        }
    }

    fun postPhotoStatus(bitmap: android.graphics.Bitmap, displayName: String, caption: String = "") {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = statusRepository.createPhotoStatus(bitmap, displayName, caption)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun postVoiceStatus(audioFile: java.io.File, durationMs: Long, displayName: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = statusRepository.createVoiceStatus(audioFile, durationMs, displayName)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun setVoiceRecording(recording: Boolean) {
        _isRecordingVoice.value = recording
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun markViewed(statusId: String) {
        viewModelScope.launch { statusRepository.markStatusViewed(statusId) }
    }
}
