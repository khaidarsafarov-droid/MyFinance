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
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.getOrNull
import com.truckerload.domain.social.LeaderboardCategory
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
    val peers: List<SocialPeerProfile> = emptyList(),
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
            socialRepository.watchPeers(),
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

    fun createPrivateChatWithPeer(peerId: String, onCreated: (String) -> Unit) {
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
                // hasMore управляется loadMore()/refreshHasMore(); не привязываем к olderMessages —
                // иначе кнопка «загрузить ещё» остаётся активной навсегда.
                hasMore = meta.hasMore,
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

    private val leaderboardCategory = MutableStateFlow(LeaderboardCategory.OVERALL)

    val leaderboard = leaderboardCategory
        .flatMapLatest { category -> socialRepository.watchLeaderboard(category) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            socialRepository.ensureInitialized()
            _state.value = _state.value.copy(
                challenge = socialRepository.weeklyChallenge(),
                challengeJoined = socialRepository.hasJoinedWeeklyChallenge(),
            )
        }
    }

    fun setLeaderboardCategory(category: LeaderboardCategory) {
        leaderboardCategory.value = category
    }

    fun refreshChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                challenge = socialRepository.weeklyChallenge(),
                challengeJoined = socialRepository.hasJoinedWeeklyChallenge(),
            )
        }
    }

    fun joinChallenge() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isJoiningChallenge = true)
            val joined = when (socialRepository.joinWeeklyChallenge()) {
                is SocialResult.Success -> true
                is SocialResult.Error -> false
            }
            _state.value = _state.value.copy(
                challenge = socialRepository.weeklyChallenge(),
                challengeJoined = joined || socialRepository.hasJoinedWeeklyChallenge(),
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
    val isRecordingVoice: Boolean = false,
    val errorMessage: String? = null,
)

class StatusViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _input = MutableStateFlow("")
    private val _isRecordingVoice = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StatusUiState> =
        combine(
            socialRepository.watchFriendStatuses(),
            _input,
            _isRecordingVoice,
            _errorMessage,
        ) { statuses, input, recording, error ->
            StatusUiState(
                statuses = statuses,
                inputText = input,
                isRecordingVoice = recording,
                errorMessage = error,
            )
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
            _errorMessage.value = null
            when (val result = socialRepository.createPhotoStatus(bitmap, displayName, caption)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun postVoiceStatus(audioFile: java.io.File, durationMs: Long, displayName: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.createVoiceStatus(audioFile, durationMs, displayName)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun setVoiceRecording(recording: Boolean) {
        _isRecordingVoice.value = recording
    }

    fun clearError() {
        _errorMessage.value = null
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
    val recommendedGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val inviteCode: String = "",
    val errorMessage: String? = null,
)

class GroupsViewModel(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _inviteCode = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(
            socialRepository.watchPublicGroups(),
            socialRepository.recommendGroups(),
            _inviteCode,
            _errorMessage,
        ) { groups, recommended, code, error ->
            GroupsUiState(
                publicGroups = groups,
                recommendedGroups = recommended.filter { rec ->
                    groups.none { it.id == rec.id } || !rec.isMember
                },
                inviteCode = code,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setInviteCode(code: String) {
        _inviteCode.value = code
    }

    fun joinGroup(chatId: String, displayName: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.joinGroup(chatId, displayName)) {
                is SocialResult.Success -> onJoined(chatId)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun joinByCode(displayName: String, onJoined: (String) -> Unit) {
        val code = _inviteCode.value.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.joinGroupByInviteCode(code, displayName)) {
                is SocialResult.Success -> onJoined(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
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
    val isBlocked: Boolean = false,
    val isUpdatingFollow: Boolean = false,
    val isBlocking: Boolean = false,
    val errorMessage: String? = null,
)

class PeerProfileViewModel(
    private val peerId: String,
    private val socialRepository: SocialRepository,
) : ViewModel() {
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

    class Factory(
        private val peerId: String,
        private val socialRepository: SocialRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PeerProfileViewModel(peerId, socialRepository) as T
    }
}
