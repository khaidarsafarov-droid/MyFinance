@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.getOrNull
import com.truckerload.domain.social.GroupInviteCode
import com.truckerload.domain.social.LeaderboardCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatusUiState(
    val statuses: List<com.truckerload.domain.social.DriverStatusPost> = emptyList(),
    val inputText: String = "",
    val isPosting: Boolean = false,
    val isRecordingVoice: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _input = MutableStateFlow("")
    private val _isRecordingVoice = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatusUiState> =
        combine(
            socialRepository.watchFriendStatuses(),
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
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun postTextStatus(displayName: String) {
        val text = _input.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            socialRepository.createTextStatus(text, displayName)
            _input.value = ""
        }
    }

    fun postPhotoStatus(bitmap: android.graphics.Bitmap, displayName: String, caption: String = "") {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.createPhotoStatus(bitmap, displayName, caption)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun postVoiceStatus(audioFile: java.io.File, durationMs: Long, displayName: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.createVoiceStatus(audioFile, durationMs, displayName)) {
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
        viewModelScope.launch { socialRepository.markStatusViewed(statusId) }
    }
}
