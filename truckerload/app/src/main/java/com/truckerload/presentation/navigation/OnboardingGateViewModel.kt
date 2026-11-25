package com.truckerload.presentation.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramOnboardingStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.repository.account.RegistrationService
import com.truckerload.data.repository.social.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * First-run profile and Telegram-token gates that used to live in [NavGraph]
 * and read Auth / profile / registration through composition locals.
 */
@HiltViewModel
class OnboardingGateViewModel @Inject constructor(
    private val authStore: AuthStore,
    userProfileStore: UserProfileStore,
    private val profileRepository: ProfileRepository,
    private val registrationService: RegistrationService,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authStore.isLoggedIn

    private val _needsSetup = MutableStateFlow<Boolean?>(null)
    val needsSetup: StateFlow<Boolean?> = _needsSetup.asStateFlow()

    private val _needsTelegramOnboarding = MutableStateFlow<Boolean?>(null)
    val needsTelegramOnboarding: StateFlow<Boolean?> = _needsTelegramOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            combine(authStore.isLoggedIn, userProfileStore.setupComplete) { loggedIn, setupComplete ->
                loggedIn to setupComplete
            }.collect { (loggedIn, setupComplete) ->
                refresh(loggedIn, setupComplete)
            }
        }
    }

    fun onSetupCompleted() {
        _needsSetup.value = false
        _needsTelegramOnboarding.value = telegramShouldPrompt()
    }

    fun onTelegramOnboardingFinished() {
        _needsTelegramOnboarding.value = false
    }

    private suspend fun refresh(loggedIn: Boolean, setupComplete: Boolean) {
        if (!loggedIn) {
            _needsSetup.value = null
            _needsTelegramOnboarding.value = null
            return
        }
        profileRepository.syncIdentityFromUserProfile()
        profileRepository.maybeMarkSetupCompleteFromExistingProfile()
        if (setupComplete) {
            _needsSetup.value = false
            _needsTelegramOnboarding.value = telegramShouldPrompt()
            return
        }
        val needsSetup = registrationService.needsRequiredOnboarding() ||
            profileRepository.needsProfileSetup()
        _needsSetup.value = needsSetup
        _needsTelegramOnboarding.value = if (needsSetup) false else telegramShouldPrompt()
    }

    private fun telegramShouldPrompt(): Boolean = TelegramOnboardingStore(appContext, authStore.currentUserIdOrNull())
        .shouldPrompt(appContext)
}
