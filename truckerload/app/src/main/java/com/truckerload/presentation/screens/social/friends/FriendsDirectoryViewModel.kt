package com.truckerload.presentation.screens.social.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.community.FriendSafetyClient
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.CommunityFriendsPublisher
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.friends.FriendRequestDirection
import com.truckerload.domain.friends.FriendRequestSendResult
import com.truckerload.domain.friends.NicknameValidator
import com.truckerload.domain.friends.establishesFriendship
import com.truckerload.domain.friends.friendCommunityLabel
import com.truckerload.domain.friends.statusKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class FriendsDirectoryViewModel @Inject constructor(
    private val authStore: AuthStore,
    chatRepository: ChatRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {

    private val friendsApi = SupabaseFriendsRealtimeService(authStore)
    private val safetyApi = FriendSafetyClient(authStore)
    private val publisher = CommunityFriendsPublisher(chatRepository, socialSyncCoordinator)

    private val _uiState = MutableStateFlow(FriendsDirectoryUiState())
    val uiState = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.watchPeers().collect { peers ->
                _uiState.update { it.copy(communityPeers = peers) }
            }
        }
        refresh(pullCommunity = true)
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                refresh(silent = true)
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    fun setSearchQuery(value: String) {
        _uiState.update {
            it.copy(searchQuery = value, searchHit = null, searchNotFound = false, statusMessage = null)
        }
    }

    fun searchFriend() {
        viewModelScope.launch {
            val q = _uiState.value.searchQuery
            if (!NicknameValidator.isValid(q)) {
                _uiState.update { it.copy(statusMessage = "invalid_search", searchHit = null, searchNotFound = false) }
                return@launch
            }
            if (!friendsApi.isConfigured()) {
                _uiState.update { it.copy(statusMessage = "need_supabase", searchHit = null) }
                return@launch
            }
            _uiState.update { it.copy(searchBusy = true, searchHit = null, searchNotFound = false) }
            val result = friendsApi.searchByNickname(q)
            val hit = result.getOrNull()
            val me = authStore.currentUserIdOrNull()
            when {
                result.isFailure -> _uiState.update {
                    it.copy(searchBusy = false, errorMessage = result.exceptionOrNull()?.message)
                }
                hit == null || hit.userId.isBlank() -> _uiState.update {
                    it.copy(searchBusy = false, searchNotFound = true, statusMessage = "not_found")
                }
                hit.userId == me -> _uiState.update {
                    it.copy(searchBusy = false, statusMessage = "self")
                }
                else -> _uiState.update {
                    it.copy(searchBusy = false, searchHit = hit, searchNotFound = false, statusMessage = null)
                }
            }
        }
    }

    fun addSearchedFriend() {
        viewModelScope.launch {
            val hit = _uiState.value.searchHit ?: return@launch
            val send = safetyApi.sendFriendRequest(hit.userId)
            val result = send.fold(
                onSuccess = { Result.success(it) },
                onFailure = { err ->
                    if (err.message == FriendSafetyClient.ERROR_SAFETY_SCHEMA_MISSING) {
                        friendsApi.addFriend(hit).map { FriendRequestSendResult.ADDED_DIRECT }
                    } else {
                        Result.failure(err)
                    }
                },
            )
            if (result.isSuccess) {
                val sendResult = result.getOrNull()
                if (sendResult?.establishesFriendship() == true) {
                    publisher.onFriendshipEstablished(
                        hit.userId,
                        friendCommunityLabel(hit.nickname, hit.displayName),
                    )
                }
                _uiState.update {
                    it.copy(
                        searchHit = null,
                        searchQuery = "",
                        statusMessage = sendResult?.statusKey() ?: "request_sent",
                    )
                }
                refresh(silent = true)
            } else {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "add failed")
                }
            }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            val request = _uiState.value.incomingRequests.find { it.id == requestId }
            safetyApi.acceptFriendRequest(requestId).onSuccess {
                if (request != null) {
                    publisher.onFriendshipEstablished(
                        request.peerId,
                        friendCommunityLabel(request.peerNickname),
                    )
                }
                _uiState.update { it.copy(statusMessage = "accepted") }
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            safetyApi.declineFriendRequest(requestId).onSuccess {
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun cancelFriendRequest(requestId: String) {
        viewModelScope.launch {
            safetyApi.cancelFriendRequest(requestId).onSuccess {
                refresh(silent = true)
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun refresh(silent: Boolean = false, pullCommunity: Boolean = false) {
        viewModelScope.launch {
            val ready = friendsApi.isConfigured()
            val requests = if (ready) {
                safetyApi.listFriendRequests().getOrElse { emptyList() }
            } else {
                emptyList()
            }
            val links = if (ready) {
                friendsApi.listMyFriendLinks().getOrElse { emptyList() }
            } else {
                emptyList()
            }
            if (pullCommunity) {
                runCatching { socialSyncCoordinator.pullRemote() }
            }
            _uiState.update {
                it.copy(
                    supabaseReady = ready,
                    incomingRequests = requests.filter { req ->
                        req.direction == FriendRequestDirection.INCOMING
                    },
                    outgoingRequests = requests.filter { req ->
                        req.direction == FriendRequestDirection.OUTGOING
                    },
                    shareLinks = links,
                    errorMessage = if (silent) it.errorMessage else null,
                )
            }
        }
    }

    companion object {
        private const val POLL_MS = 20_000L
    }
}
