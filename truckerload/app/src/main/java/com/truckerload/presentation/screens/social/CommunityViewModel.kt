@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.LeaderboardCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CommunityUiState(
    val challenge: Challenge? = null,
    val challengeJoined: Boolean = false,
    val isJoiningChallenge: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class CommunityViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _state.asStateFlow()

    private val leaderboardCategory = MutableStateFlow(LeaderboardCategory.OVERALL)

    val leaderboard = leaderboardCategory
        .flatMapLatest { category -> socialRepository.watchLeaderboard(category) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                socialRepository.ensureInitialized()
                _state.value = _state.value.copy(
                    challenge = socialRepository.weeklyChallenge(),
                    challengeJoined = socialRepository.hasJoinedWeeklyChallenge(),
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(isLoading = false, errorMessage = error.toUiMessage())
            }
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
                    challenge = socialRepository.weeklyChallenge(),
                    challengeJoined = socialRepository.hasJoinedWeeklyChallenge(),
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
            when (val result = socialRepository.joinWeeklyChallenge()) {
                is SocialResult.Success -> {
                    _state.value = _state.value.copy(
                        challenge = socialRepository.weeklyChallenge(),
                        challengeJoined = true,
                        isJoiningChallenge = false,
                    )
                }
                is SocialResult.Error -> {
                    _state.value = _state.value.copy(
                        challengeJoined = socialRepository.hasJoinedWeeklyChallenge(),
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

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CommunityViewModel(socialRepository) as T
    }
}

