package com.truckerload.presentation.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.UserProfile
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.social.EnhancedDriverProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Social + local profile used for the home welcome name and the tablet rail. */
@HiltViewModel
@Suppress("ktlint:standard:class-signature", "ktlint:standard:function-signature")
class DriverIdentityViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    userProfileStore: UserProfileStore,
) : ViewModel() {
    val socialProfile: StateFlow<EnhancedDriverProfile?> =
        profileRepository.watchMyEnhancedProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val profile: StateFlow<UserProfile?> = userProfileStore.profile

    val setupComplete: StateFlow<Boolean> = userProfileStore.setupComplete
    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
