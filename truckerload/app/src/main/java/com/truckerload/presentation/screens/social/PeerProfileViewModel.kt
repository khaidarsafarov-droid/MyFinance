package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.SocialResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val peerId = Uri.decode(savedStateHandle.get<String>("peerId").orEmpty())
    private val _followUpdating = MutableStateFlow(false)
    private val _blocking = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // kotlinx.coroutines combine поддерживает до 5 Flow; вкладываем 3+3.
    val uiState: StateFlow<PeerProfileUiState> =
        combine(
            combine(
                socialRepository.watchPeer(peerId),
                socialRepository.watchIsFollowing(peerId),
                socialRepository.watchIsBlocked(peerId),
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
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            _followUpdating.value = true
            _errorMessage.value = null
            val result = if (uiState.value.isFollowing) {
                socialRepository.unfollowDriver(peerId)
            } else {
                socialRepository.followDriver(peerId)
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
                socialRepository.unblockUser(peerId)
            } else {
                socialRepository.blockUser(peerId)
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
            when (val result = socialRepository.createPrivateChatWithPeer(peerId)) {
                is SocialResult.Success -> onCreated(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
