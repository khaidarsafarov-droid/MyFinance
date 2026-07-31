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

data class ProfileUiState(
    val profile: EnhancedDriverProfile? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null,
    val saveError: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _avatarActionState = MutableStateFlow(AvatarActionState())

    val uiState: StateFlow<ProfileUiState> =
        combine(
            socialRepository.watchMyEnhancedProfile(),
            _avatarActionState,
        ) { profile, avatarState ->
            ProfileUiState(
                profile = profile,
                isUploadingAvatar = avatarState.isUploading,
                avatarError = avatarState.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _editState = MutableStateFlow<ProfileUiState?>(null)
    val editState: StateFlow<ProfileUiState?> = _editState.asStateFlow()

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun startEdit() {
        _editState.value = uiState.value
    }

    fun cancelEdit() {
        _editState.value = null
    }

    fun uploadAvatar(bitmap: Bitmap) {
        viewModelScope.launch {
            _avatarActionState.update { it.copy(isUploading = true, error = null) }
            when (val result = socialRepository.uploadAvatar(bitmap)) {
                is SocialResult.Success -> _avatarActionState.update { it.copy(isUploading = false) }
                is SocialResult.Error -> _avatarActionState.update {
                    it.copy(isUploading = false, error = result.message)
                }
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            _avatarActionState.update { it.copy(isUploading = true, error = null) }
            when (val result = socialRepository.removeAvatar()) {
                is SocialResult.Success -> _avatarActionState.update { it.copy(isUploading = false) }
                is SocialResult.Error -> _avatarActionState.update {
                    it.copy(isUploading = false, error = result.message)
                }
            }
        }
    }

    fun clearAvatarError() {
        _avatarActionState.update { it.copy(error = null) }
    }

    fun saveEdit(
        displayName: String,
        truckType: String,
        experienceYears: Int,
        homeState: String,
        routes: String,
        about: String,
        status: DriverStatus,
        licenseClass: String = "",
        phoneNumber: String = "",
        telegramUsername: String = "",
        whatsappNumber: String = "",
        specialties: String = "",
    ) {
        viewModelScope.launch {
            _editState.value = _editState.value?.copy(isSaving = true, saveError = null)
            when (val result = socialRepository.updateProfile(
                    displayName = displayName.trim(),
                    truckType = truckType.trim(),
                    experienceYears = experienceYears.coerceAtLeast(0),
                    homeState = homeState.trim().uppercase().take(2),
                    routes = routes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    about = about.trim(),
                    status = status,
                    licenseClass = licenseClass.trim(),
                    phoneNumber = phoneNumber.trim().ifBlank { null },
                    telegramUsername = telegramUsername.trim().ifBlank { null },
                    whatsappNumber = whatsappNumber.trim().ifBlank { null },
                    specialties = specialties.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            )) {
                is SocialResult.Success -> _editState.value = null
                is SocialResult.Error -> {
                    _editState.value = _editState.value?.copy(isSaving = false, saveError = result.message)
                }
            }
        }
    }
}

private data class AvatarActionState(
    val isUploading: Boolean = false,
    val error: String? = null,
)
