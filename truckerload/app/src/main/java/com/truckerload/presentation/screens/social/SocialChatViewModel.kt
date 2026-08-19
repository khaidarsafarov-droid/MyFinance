package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.MediaRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.repository.social.SocialConstants
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.data.voice.CallHistory
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.CommunityReportReason
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.getOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val errorMessage: String? = null,
    val peerId: String? = null,
    val isPrivate: Boolean = false,
    val isBlocked: Boolean = false,
    val isContact: Boolean = false,
    val isGroupManager: Boolean = false,
    val peerName: String = "",
) {
    val allMessages: List<SocialMessage> =
        (olderMessages.filter { older -> messages.none { it.id == older.id } } + messages)
            .sortedWith(compareBy({ it.sentAt }, { it.id }))
}

@HiltViewModel
class SocialChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    private val mediaRepository: MediaRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
    private val voiceRepository: VoiceRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val chatId = Uri.decode(savedStateHandle.get<String>("chatId").orEmpty())

    private data class ChatMeta(
        val title: String = "",
        val participantCount: Int = 0,
        val chatRating: Double = 0.0,
        val onlineCount: Int = 0,
        val olderMessages: List<SocialMessage> = emptyList(),
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val replyTo: SocialMessage? = null,
        val errorMessage: String? = null,
        val peerId: String? = null,
        val isPrivate: Boolean = false,
        val isBlocked: Boolean = false,
        val isContact: Boolean = false,
        val isGroupManager: Boolean = false,
        val peerName: String = "",
    )

    private val _input = MutableStateFlow("")
    private val _meta = MutableStateFlow(ChatMeta())

    val uiState: StateFlow<SocialChatUiState> =
        combine(
            chatRepository.watchMessages(chatId),
            _input,
            _meta,
            profileRepository.watchMyProfile(),
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
                errorMessage = meta.errorMessage,
                peerId = meta.peerId,
                isPrivate = meta.isPrivate,
                isBlocked = meta.isBlocked,
                isContact = meta.isContact,
                isGroupManager = meta.isGroupManager,
                peerName = meta.peerName,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SocialChatUiState())

    init {
        viewModelScope.launch {
            socialSyncCoordinator.ensureInitialized()
            chatRepository.markChatRead(chatId)
            val chat = chatRepository.getChat(chatId)
            val peerId = chatRepository.privatePeerId(chatId)
            val members = if (chat?.type == ChatType.GROUP) {
                runCatching { groupRepository.watchGroupMembers(chatId).first() }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
            val me = members.find { it.isMe }
            _meta.value = _meta.value.copy(
                title = chat?.title ?: "",
                participantCount = chat?.participantCount ?: 0,
                chatRating = chat?.rating ?: 0.0,
                onlineCount = chat?.onlineCount ?: 0,
                peerId = peerId,
                isPrivate = chat?.type == ChatType.PRIVATE,
                isGroupManager = me?.role == "OWNER" || me?.role == "MODERATOR" ||
                    (chat?.creatorId?.isNotBlank() == true && chat.creatorId == me?.userId),
                peerName = chat?.title.orEmpty(),
            )
            if (peerId != null) {
                launch {
                    combine(
                        profileRepository.watchIsBlocked(peerId),
                        profileRepository.watchIsFollowing(peerId),
                    ) { blocked, following -> blocked to following }
                        .collect { (blocked, following) ->
                            _meta.value = _meta.value.copy(isBlocked = blocked, isContact = following)
                        }
                }
            }
            refreshHasMore()
            while (isActive) {
                delay(2_000)
                runCatching { socialSyncCoordinator.pullRemote() }
            }
        }
    }

    private suspend fun refreshHasMore() {
        val currentOldest = uiState.value.allMessages.firstOrNull()?.sentAt ?: return
        val page = chatRepository.loadMoreMessages(chatId, currentOldest).getOrNull().orEmpty()
        _meta.value = _meta.value.copy(hasMore = page.isNotEmpty())
    }

    fun setInput(text: String) {
        _input.value = text
    }

    fun sendMessage() {
        val text = _input.value
        if (text.isBlank()) return
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Me" }
            val replyId = _meta.value.replyTo?.id
            when (val result = chatRepository.sendMessage(chatId, text, name, replyToId = replyId)) {
                is SocialResult.Success -> {
                    _input.value = ""
                    _meta.value = _meta.value.copy(replyTo = null, errorMessage = null)
                }
                is SocialResult.Error -> {
                    _meta.value = _meta.value.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun sendImage(bitmap: android.graphics.Bitmap, caption: String = "") {
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Me" }
            mediaRepository.sendImageMessage(chatId, bitmap, caption, name)
        }
    }

    fun sendVoiceNote(file: java.io.File, durationMs: Long) {
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Me" }
            mediaRepository.sendVoiceMessage(chatId, file, durationMs, name)
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
            chatRepository.addReaction(messageId, reaction)
        }
    }

    fun blockPeer(onDone: () -> Unit) {
        val peer = _meta.value.peerId ?: return
        viewModelScope.launch {
            when (val result = profileRepository.blockUser(peer)) {
                is SocialResult.Success -> onDone()
                is SocialResult.Error -> _meta.value = _meta.value.copy(errorMessage = result.message)
            }
        }
    }

    fun reportPeer(reason: CommunityReportReason) {
        val peer = _meta.value.peerId ?: return
        viewModelScope.launch {
            when (val result = profileRepository.reportUser(peer, reason, chatId = chatId)) {
                is SocialResult.Success ->
                    _meta.value = _meta.value.copy(errorMessage = "reported")
                is SocialResult.Error ->
                    _meta.value = _meta.value.copy(errorMessage = result.message)
            }
        }
    }

    fun loadMore() {
        if (_meta.value.isLoadingMore) return
        val oldest = uiState.value.allMessages.firstOrNull()?.sentAt ?: return
        viewModelScope.launch {
            _meta.value = _meta.value.copy(isLoadingMore = true)
            when (val result = chatRepository.loadMoreMessages(chatId, oldest)) {
                is SocialResult.Success -> {
                    val batch = result.data
                    if (batch.isNotEmpty()) {
                        _meta.value = _meta.value.copy(
                            olderMessages = batch + _meta.value.olderMessages,
                        )
                    }
                    _meta.value = _meta.value.copy(
                        hasMore = batch.size >= SocialConstants.MESSAGE_PAGE_SIZE,
                        isLoadingMore = false,
                    )
                }
                is SocialResult.Error -> {
                    _meta.value = _meta.value.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun startGroupCall(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val title = uiState.value.chatTitle.ifBlank { "Group call" }
            voiceRepository.ensureGroupRoom(chatId, title)
                .onSuccess { roomId ->
                    runCatching {
                        CallHistory.recordGroupStart(chatRepository, profileRepository, chatId)
                    }
                    onReady(roomId)
                }
                .onFailure { error ->
                    _meta.value = _meta.value.copy(errorMessage = error.message)
                }
        }
    }
}
