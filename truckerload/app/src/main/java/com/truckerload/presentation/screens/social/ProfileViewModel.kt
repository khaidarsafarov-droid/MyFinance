package com.truckerload.presentation.screens.social

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.R
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.utils.AnalyticsOwnerName
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: EnhancedDriverProfile? = null,
    val givenName: String = "",
    val familyName: String = "",
    val nameMessage: String? = null,
    val nameMessageIsError: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val userProfileStore: UserProfileStore,
    private val app: Application,
) : ViewModel() {

    private val nameFeedback = MutableStateFlow<Pair<String, Boolean>?>(null)

    val uiState: StateFlow<ProfileUiState> =
        combine(
            profileRepository.watchMyEnhancedProfile(),
            userProfileStore.profile,
            nameFeedback,
        ) { profile, user, feedback ->
            val (given, family) = AnalyticsOwnerName.fromProfile(
                givenName = user?.givenName,
                familyName = user?.familyName,
                email = user?.email,
                socialDisplayName = profile.displayName,
            )
            ProfileUiState(
                profile = profile,
                givenName = given,
                familyName = family,
                nameMessage = feedback?.first,
                nameMessageIsError = feedback?.second == true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    init {
        viewModelScope.launch {
            profileRepository.syncIdentityFromUserProfile()
            profileRepository.maybeMarkSetupCompleteFromExistingProfile()
        }
    }

    fun saveName(givenName: String, familyName: String) {
        viewModelScope.launch {
            val given = givenName.trim()
            val family = familyName.trim()
            if (AnalyticsOwnerName.display(given, family).isBlank()) {
                nameFeedback.value = app.getString(R.string.profile_name_required) to true
                return@launch
            }
            runCatching { profileRepository.updateOwnName(given, family) }
                .onSuccess {
                    nameFeedback.value = app.getString(R.string.profile_name_saved) to false
                }
                .onFailure {
                    nameFeedback.value = app.getString(R.string.social_error_save_profile) to true
                }
        }
    }
}
