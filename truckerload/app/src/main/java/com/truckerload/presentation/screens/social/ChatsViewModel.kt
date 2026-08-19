@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatsUiState(
    val chats: List<SocialChat> = emptyList(),
    val groupChats: List<SocialChat> = emptyList(),
    val privateChats: List<SocialChat> = emptyList(),
    val peers: List<SocialPeerProfile> = emptyList(),
    val searchQuery: String = "",
    val totalUnread: Int = 0,
    val isCreatingChat: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatsUiState> =
        combine(
            searchQuery.flatMapLatest { query -> chatRepository.watchChatsSearch(query) },
            chatRepository.watchTotalUnread(),
            chatRepository.watchPeers(),
            searchQuery,
            _errorMessage,
        ) { chats, unread, peers, query, error ->
            ChatsUiState(
                chats = chats,
                groupChats = chats.filter { it.type != ChatType.PRIVATE },
                privateChats = chats.filter { it.type == ChatType.PRIVATE },
                peers = peers,
                searchQuery = query,
                totalUnread = unread,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatsUiState())

    init {
        viewModelScope.launch {
            socialSyncCoordinator.ensureInitialized()
            while (isActive) {
                delay(4_000)
                runCatching { socialSyncCoordinator.pullRemote() }
            }
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun createGroupChat(name: String, description: String = "", onCreated: (String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.createGroupChat(trimmed, description = description.trim())) {
                is SocialResult.Success -> onCreated(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun createPrivateChat(name: String, onCreated: (String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = chatRepository.createPrivateChat(trimmed)) {
                is SocialResult.Success -> onCreated(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun createPrivateChatWithPeer(peerId: String, onCreated: (String) -> Unit) {
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
}
