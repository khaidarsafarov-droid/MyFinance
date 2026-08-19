@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult

data class PeerProfileUiState(
    val peer: com.truckerload.domain.social.SocialPeerProfile? = null,
    val isFollowing: Boolean = false,
    val isBlocked: Boolean = false,
    val isUpdatingFollow: Boolean = false,
    val isBlocking: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class PeerProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {
    private val peerId = Uri.decode(savedStateHandle.get<String>("peerId").orEmpty())
    private val _followUpdating = MutableStateFlow(false)
    private val _blocking = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // kotlinx.coroutines combine поддерживает до 5 Flow; вкладываем 3+3.
    val uiState: StateFlow<PeerProfileUiState> =
        combine(
            combine(
                profileRepository.watchPeer(peerId),
                profileRepository.watchIsFollowing(peerId),
                profileRepository.watchIsBlocked(peerId),
            ) { peer, isFollowing, isBlocked -> Triple(peer, isFollowing, isBlocked) },
            combine(_followUpdating, _blocking, _errorMessage) { updating, blocking, error ->
                Triple(updating, blocking, error)
            },
        ) { (peer, isFollowing, isBlocked), (updating, blocking, error) ->
            PeerProfileUiState(
                peer = peer,
                isFollowing = isFollowing,
                isBlocked = isBlocked,
                isUpdatingFollow = updating,
                isBlocking = blocking,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeerProfileUiState())

    init {
        viewModelScope.launch { socialSyncCoordinator.ensureInitialized() }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            _followUpdating.value = true
            _errorMessage.value = null
            val result = if (uiState.value.isFollowing) {
                profileRepository.unfollowDriver(peerId)
            } else {
                profileRepository.followDriver(peerId)
            }
            if (result is SocialResult.Error) {
                _errorMessage.value = result.message
            }
            _followUpdating.value = false
        }
    }

    fun toggleBlock() {
        viewModelScope.launch {
            _blocking.value = true
            _errorMessage.value = null
            val result = if (uiState.value.isBlocked) {
                profileRepository.unblockUser(peerId)
            } else {
                profileRepository.blockUser(peerId)
            }
            if (result is SocialResult.Error) {
                _errorMessage.value = result.message
            }
            _blocking.value = false
        }
    }

    fun startPrivateChat(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = chatRepository.createPrivateChatWithPeer(peerId)) {
                is SocialResult.Success -> onCreated(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun reportPeer(reason: com.truckerload.domain.social.CommunityReportReason) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = profileRepository.reportUser(peerId, reason)) {
                is SocialResult.Success -> _errorMessage.value = "reported"
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }
}
