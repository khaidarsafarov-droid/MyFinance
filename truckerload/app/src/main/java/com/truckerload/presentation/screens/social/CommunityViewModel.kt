@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.crowd.CrowdRpmMapper
import com.truckerload.domain.crowd.CrowdRpmWeekSummary
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.LeaderboardCategory
import com.truckerload.domain.social.SocialResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CommunityUiState(
    val challenge: Challenge? = null,
    val challengeJoined: Boolean = false,
    val isJoiningChallenge: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showCrowdConsent: Boolean = false,
)

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
    private val settingsDataStore: SettingsDataStore,
    private val loadRepository: LoadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _state.asStateFlow()

    private val leaderboardCategory = MutableStateFlow(LeaderboardCategory.OVERALL)

    val leaderboard = leaderboardCategory
        .flatMapLatest { category -> profileRepository.watchLeaderboard(category) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val crowdStatsOptIn: StateFlow<Boolean> = settingsDataStore.crowdStatsOptIn.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    val crowdWeekSummary: StateFlow<CrowdRpmWeekSummary?> = combine(
        crowdStatsOptIn,
        loadRepository.watchLoads(),
    ) { optedIn, loads ->
        if (!optedIn) null else CrowdRpmMapper.weekSummary(loads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            if (!settingsDataStore.isCrowdStatsPromptSeenOnce()) {
                _state.value = _state.value.copy(showCrowdConsent = true)
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                socialSyncCoordinator.ensureInitialized()
                _state.value = _state.value.copy(
                    challenge = profileRepository.weeklyChallenge(),
                    challengeJoined = profileRepository.hasJoinedWeeklyChallenge(),
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.toUiMessage())
            }
            while (isActive) {
                delay(4_000)
                runCatching {
                    socialSyncCoordinator.pullRemote()
                    _state.value = _state.value.copy(
                        challenge = profileRepository.weeklyChallenge(),
                        challengeJoined = profileRepository.hasJoinedWeeklyChallenge(),
                    )
                }
            }
        }
    }

    fun acceptCrowdStats() {
        viewModelScope.launch {
            settingsDataStore.saveCrowdStatsOptIn(true)
            _state.value = _state.value.copy(showCrowdConsent = false)
        }
    }

    fun declineCrowdStats() {
        viewModelScope.launch {
            settingsDataStore.saveCrowdStatsOptIn(false)
            _state.value = _state.value.copy(showCrowdConsent = false)
        }
    }

    fun setLeaderboardCategory(category: LeaderboardCategory) {
        leaderboardCategory.value = category
    }

    fun refreshChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                _state.value = _state.value.copy(
                    challenge = profileRepository.weeklyChallenge(),
                    challengeJoined = profileRepository.hasJoinedWeeklyChallenge(),
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.toUiMessage())
            }
        }
    }

    fun joinChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoiningChallenge = true, errorMessage = null)
            when (val result = profileRepository.joinWeeklyChallenge()) {
                is SocialResult.Success -> {
                    _state.value = _state.value.copy(
                        challenge = profileRepository.weeklyChallenge(),
                        challengeJoined = true,
                        isJoiningChallenge = false,
                    )
                }
                is SocialResult.Error -> {
                    _state.value = _state.value.copy(
                        challengeJoined = profileRepository.hasJoinedWeeklyChallenge(),
                        isJoiningChallenge = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }
}
