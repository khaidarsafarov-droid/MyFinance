package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.Challenge
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.getOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: EnhancedDriverProfile? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null,
)

class ProfileViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _avatarActionState = MutableStateFlow(AvatarActionState())

    val uiState: StateFlow<ProfileUiState> =
        combine(
            socialRepository.watchMyEnhancedProfile(),
            _avatarActionState,
        ) { profile, avatarState ->
            ProfileUiState(
                profile = profile,
                isUploadingAvatar = avatarState.isUploading,
                avatarError = avatarState.error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _editState = MutableStateFlow<ProfileUiState?>(null)
    val editState: StateFlow<ProfileUiState?> = _editState.asStateFlow()

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
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
            when (val result = socialRepository.uploadAvatar(bitmap)) {
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
            when (val result = socialRepository.removeAvatar()) {
                is SocialResult.Success -> _avatarActionState.update { it.copy(isUploading = false) }
                is SocialResult.Error -> _avatarActionState.update {
                    it.copy(isUploading = false, error = result.message)
                }
            }
        }
    }

    fun clearAvatarError() {
        _avatarActionState.update { it.copy(error = null) }
    }

    fun saveEdit(
        displayName: String,
        truckType: String,
        experienceYears: Int,
        homeState: String,
        routes: String,
        about: String,
        status: DriverStatus,
        licenseClass: String = "A",
        phoneNumber: String = "",
        telegramUsername: String = "",
        whatsappNumber: String = "",
        specialties: String = "",
    ) {
        viewModelScope.launch {
            _editState.value = _editState.value?.copy(isSaving = true)
            socialRepository.updateProfile(
                displayName = displayName.trim(),
                truckType = truckType.trim(),
                experienceYears = experienceYears.coerceAtLeast(0),
                homeState = homeState.trim().uppercase(),
                routes = routes.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                about = about.trim(),
                status = status,
                licenseClass = licenseClass.trim().ifBlank { "A" },
                phoneNumber = phoneNumber.trim().ifBlank { null },
                telegramUsername = telegramUsername.trim().ifBlank { null },
                whatsappNumber = whatsappNumber.trim().ifBlank { null },
                specialties = specialties.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            )
            _editState.value = null
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(socialRepository) as T
    }
}

private data class AvatarActionState(
    val isUploading: Boolean = false,
    val error: String? = null,
)

data class ChatsUiState(
    val chats: List<SocialChat> = emptyList(),
    val groupChats: List<SocialChat> = emptyList(),
    val privateChats: List<SocialChat> = emptyList(),
    val searchQuery: String = "",
    val totalUnread: Int = 0,
    val isCreatingChat: Boolean = false,
    val errorMessage: String? = null,
)

class ChatsViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatsUiState> =
        combine(
            searchQuery.flatMapLatest { query -> socialRepository.watchChatsSearch(query) },
            socialRepository.watchTotalUnread(),
            searchQuery,
            _errorMessage,
        ) { chats, unread, query, error ->
            ChatsUiState(
                chats = chats,
                groupChats = chats.filter { it.type != ChatType.PRIVATE },
                privateChats = chats.filter { it.type == ChatType.PRIVATE },
                searchQuery = query,
                totalUnread = unread,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChatsUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun createGroupChat(name: String, onCreated: (String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.createGroupChat(trimmed)) {
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
            when (val result = socialRepository.createPrivateChat(trimmed)) {
                is SocialResult.Success -> onCreated(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChatsViewModel(socialRepository) as T
    }
}

data class SocialChatUiState(
    val chatTitle: String = "",
    val participantCount: Int = 0,
    val chatRating: Double = 0.0,
    val onlineCount: Int = 0,
    val messages: List<SocialMessage> = emptyList(),
    val olderMessages: List<SocialMessage> = emptyList(),
    val inputText: String = "",
    val myDisplayName: String = "",
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val replyTo: SocialMessage? = null,
) {
    val allMessages: List<SocialMessage> = olderMessages + messages
}

class SocialChatViewModel(
    private val chatId: String,
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private data class ChatMeta(
        val title: String = "",
        val participantCount: Int = 0,
        val chatRating: Double = 0.0,
        val onlineCount: Int = 0,
        val olderMessages: List<SocialMessage> = emptyList(),
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val replyTo: SocialMessage? = null,
    )

    private val _input = MutableStateFlow("")
    private val _meta = MutableStateFlow(ChatMeta())

    val uiState: StateFlow<SocialChatUiState> =
        combine(
            socialRepository.watchMessages(chatId),
            _input,
            _meta,
            socialRepository.watchMyProfile(),
        ) { messages, input, meta, profile ->
            SocialChatUiState(
                chatTitle = meta.title,
                participantCount = meta.participantCount,
                chatRating = meta.chatRating,
                onlineCount = meta.onlineCount,
                messages = messages,
                olderMessages = meta.olderMessages,
                inputText = input,
                myDisplayName = profile.displayName,
                isLoadingMore = meta.isLoadingMore,
                hasMore = meta.hasMore || meta.olderMessages.isNotEmpty(),
                replyTo = meta.replyTo,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SocialChatUiState())

    init {
        viewModelScope.launch {
            socialRepository.ensureInitialized()
            socialRepository.markChatRead(chatId)
            val chat = socialRepository.getChat(chatId)
            _meta.value = _meta.value.copy(
                title = chat?.title ?: "",
                participantCount = chat?.participantCount ?: 0,
                chatRating = chat?.rating ?: 0.0,
                onlineCount = chat?.onlineCount ?: 0,
            )
            refreshHasMore()
        }
    }

    private suspend fun refreshHasMore() {
        val currentOldest = uiState.value.allMessages.firstOrNull()?.sentAt ?: return
        val page = socialRepository.loadMoreMessages(chatId, currentOldest).getOrNull().orEmpty()
        _meta.value = _meta.value.copy(hasMore = page.isNotEmpty())
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun sendMessage() {
        val text = _input.value
        if (text.isBlank()) return
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Я" }
            val replyId = _meta.value.replyTo?.id
            socialRepository.sendMessage(chatId, text, name, replyToId = replyId)
            _input.value = ""
            _meta.value = _meta.value.copy(replyTo = null)
        }
    }

    fun sendImage(bitmap: android.graphics.Bitmap, caption: String = "") {
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Я" }
            socialRepository.sendImageMessage(chatId, bitmap, caption, name)
        }
    }

    fun sendVoiceNote(file: java.io.File, durationMs: Long) {
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Я" }
            socialRepository.sendVoiceMessage(chatId, file, durationMs, name)
        }
    }

    fun setReplyTo(message: SocialMessage?) {
        _meta.value = _meta.value.copy(replyTo = message)
    }

    fun cancelReply() {
        _meta.value = _meta.value.copy(replyTo = null)
    }

    fun addReaction(messageId: String, reaction: String) {
        viewModelScope.launch {
            socialRepository.addReaction(messageId, reaction)
        }
    }

    fun loadMore() {
        if (_meta.value.isLoadingMore) return
        val oldest = uiState.value.allMessages.firstOrNull()?.sentAt ?: return
        viewModelScope.launch {
            _meta.value = _meta.value.copy(isLoadingMore = true)
            when (val result = socialRepository.loadMoreMessages(chatId, oldest)) {
                is SocialResult.Success -> {
                    val batch = result.data
                    if (batch.isNotEmpty()) {
                        _meta.value = _meta.value.copy(
                            olderMessages = batch + _meta.value.olderMessages,
                        )
                    }
                    _meta.value = _meta.value.copy(
                        hasMore = batch.size >= SocialRepository.MESSAGE_PAGE_SIZE,
                        isLoadingMore = false,
                    )
                }
                is SocialResult.Error -> {
                    _meta.value = _meta.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    class Factory(
        private val chatId: String,
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SocialChatViewModel(chatId, socialRepository) as T
    }
}

data class CommunityUiState(
    val challenge: Challenge? = null,
    val challengeJoined: Boolean = false,
    val isJoiningChallenge: Boolean = false,
)

class CommunityViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _state.asStateFlow()

    val leaderboard = socialRepository.watchLeaderboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            socialRepository.ensureInitialized()
            _state.value = _state.value.copy(challenge = socialRepository.weeklyChallenge())
        }
    }

    fun refreshChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(challenge = socialRepository.weeklyChallenge())
        }
    }

    fun joinChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoiningChallenge = true)
            socialRepository.joinWeeklyChallenge()
            _state.value = _state.value.copy(
                challenge = socialRepository.weeklyChallenge(),
                challengeJoined = true,
                isJoiningChallenge = false,
            )
        }
    }

    class Factory(
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CommunityViewModel(socialRepository) as T
    }
}

data class StatusUiState(
    val statuses: List<com.truckerload.domain.social.DriverStatusPost> = emptyList(),
    val inputText: String = "",
    val isPosting: Boolean = false,
)

class StatusViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _input = MutableStateFlow("")
    val uiState: StateFlow<StatusUiState> =
        combine(
            socialRepository.watchFriendStatuses(),
            _input,
        ) { statuses, input ->
            StatusUiState(statuses = statuses, inputText = input)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun postTextStatus(displayName: String) {
        val text = _input.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            socialRepository.createTextStatus(text, displayName)
            _input.value = ""
        }
    }

    fun postPhotoStatus(bitmap: android.graphics.Bitmap, displayName: String, caption: String = "") {
        viewModelScope.launch {
            socialRepository.createPhotoStatus(bitmap, displayName, caption)
        }
    }

    fun markViewed(statusId: String) {
        viewModelScope.launch { socialRepository.markStatusViewed(statusId) }
    }

    class Factory(private val socialRepository: SocialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatusViewModel(socialRepository) as T
    }
}

data class GroupsUiState(
    val publicGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val inviteCode: String = "",
)

class GroupsViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _inviteCode = MutableStateFlow("")
    val uiState: StateFlow<GroupsUiState> =
        combine(
            socialRepository.watchPublicGroups(),
            _inviteCode,
        ) { groups, code ->
            GroupsUiState(publicGroups = groups, inviteCode = code)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setInviteCode(code: String) {
        _inviteCode.value = code
    }

    fun joinGroup(chatId: String, displayName: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            when (val result = socialRepository.joinGroup(chatId, displayName)) {
                is com.truckerload.domain.social.SocialResult.Success -> onJoined(chatId)
                is com.truckerload.domain.social.SocialResult.Error -> Unit
            }
        }
    }

    fun joinByCode(displayName: String, onJoined: (String) -> Unit) {
        val code = _inviteCode.value.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            when (val result = socialRepository.joinGroupByInviteCode(code, displayName)) {
                is com.truckerload.domain.social.SocialResult.Success -> onJoined(result.data)
                is com.truckerload.domain.social.SocialResult.Error -> Unit
            }
        }
    }

    class Factory(private val socialRepository: SocialRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GroupsViewModel(socialRepository) as T
    }
}

data class GroupDetailUiState(
    val chat: com.truckerload.domain.social.SocialChat? = null,
    val members: List<com.truckerload.domain.social.ChatMember> = emptyList(),
)

class GroupDetailViewModel(
    private val chatId: String,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    val uiState: StateFlow<GroupDetailUiState> =
        combine(
            socialRepository.watchChats().map { chats -> chats.firstOrNull { it.id == chatId } },
            socialRepository.watchGroupMembers(chatId),
        ) { chat, members ->
            GroupDetailUiState(chat = chat, members = members)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    fun leaveGroup(onLeft: () -> Unit) {
        viewModelScope.launch {
            socialRepository.leaveGroup(chatId)
            onLeft()
        }
    }

    class Factory(
        private val chatId: String,
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GroupDetailViewModel(chatId, socialRepository) as T
    }
}

data class PeerProfileUiState(
    val peer: com.truckerload.domain.social.SocialPeerProfile? = null,
    val isFollowing: Boolean = false,
    val isUpdatingFollow: Boolean = false,
)

class PeerProfileViewModel(
    private val peerId: String,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _followUpdating = MutableStateFlow(false)

    val uiState: StateFlow<PeerProfileUiState> =
        combine(
            socialRepository.watchPeer(peerId),
            socialRepository.watchIsFollowing(peerId),
            _followUpdating,
        ) { peer, isFollowing, updating ->
            PeerProfileUiState(peer = peer, isFollowing = isFollowing, isUpdatingFollow = updating)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeerProfileUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            _followUpdating.value = true
            if (uiState.value.isFollowing) {
                socialRepository.unfollowDriver(peerId)
            } else {
                socialRepository.followDriver(peerId)
            }
            _followUpdating.value = false
        }
    }

    class Factory(
        private val peerId: String,
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PeerProfileViewModel(peerId, socialRepository) as T
    }
}
