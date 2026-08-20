package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.SocialResult

data class ProfileUiState(
    val profile: EnhancedDriverProfile? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null,
    val saveError: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {

    private val _avatarActionState = MutableStateFlow(AvatarActionState())

    val uiState: StateFlow<ProfileUiState> =
        combine(
            profileRepository.watchMyEnhancedProfile(),
            _avatarActionState,
        ) { profile, avatarState ->
            ProfileUiState(
                profile = profile,
                isSaving = avatarState.isSaving,
                isUploadingAvatar = avatarState.isUploading,
                avatarError = avatarState.error,
                saveError = avatarState.saveError,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _editState = MutableStateFlow<ProfileUiState?>(null)
    val editState: StateFlow<ProfileUiState?> = _editState.asStateFlow()

    init {
        viewModelScope.launch { socialSyncCoordinator.ensureInitialized() }
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
            when (val result = profileRepository.uploadAvatar(bitmap)) {
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
            when (val result = profileRepository.removeAvatar()) {
                is SocialResult.Success -> _avatarActionState.update { it.copy(isUploading = false) }
                is SocialResult.Error -> _avatarActionState.update {
                    it.copy(isUploading = false, error = result.message)
                }
            }
        }
    }

    fun clearAvatarError() {
        _avatarActionState.update { it.copy(error = null, saveError = null) }
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
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            _avatarActionState.update { it.copy(isSaving = true, saveError = null) }
            val current = uiState.value.profile
            when (val result = profileRepository.updateProfile(
                    displayName = displayName.trim(),
                    truckType = truckType.trim(),
                    experienceYears = experienceYears.coerceAtLeast(0),
                    homeState = homeState.trim().uppercase().take(2),
                    routes = routes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    about = about.trim(),
                    status = status,
                    licenseClass = licenseClass.trim(),
                    endorsements = current?.endorsements.orEmpty(),
                    phoneNumber = phoneNumber.trim().ifBlank { null },
                    telegramUsername = telegramUsername.trim().ifBlank { null },
                    whatsappNumber = whatsappNumber.trim().ifBlank { null },
                    specialties = specialties.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    maxRadius = current?.maxRadius ?: 500,
            )) {
                is SocialResult.Success -> {
                    _avatarActionState.update { it.copy(isSaving = false) }
                    _editState.value = null
                    onResult(true)
                }
                is SocialResult.Error -> {
                    _avatarActionState.update {
                        it.copy(isSaving = false, saveError = result.message)
                    }
                    _editState.value = _editState.value?.copy(isSaving = false, saveError = result.message)
                    onResult(false)
                }
            }
        }
    }
}

private data class AvatarActionState(
    val isUploading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
)
