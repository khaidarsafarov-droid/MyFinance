@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.truckerload.presentation.screens.social

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import com.truckerload.domain.social.GroupInviteCode
import com.truckerload.domain.social.LeaderboardCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) {
    val allMessages: List<SocialMessage> = olderMessages + messages
}

@HiltViewModel
class SocialChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
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
                errorMessage = meta.errorMessage,
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
            val name = uiState.value.myDisplayName.ifBlank { "Me" }
            val replyId = _meta.value.replyTo?.id
            when (val result = socialRepository.sendMessage(chatId, text, name, replyToId = replyId)) {
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
            socialRepository.sendImageMessage(chatId, bitmap, caption, name)
        }
    }

    fun sendVoiceNote(file: java.io.File, durationMs: Long) {
        viewModelScope.launch {
            val name = uiState.value.myDisplayName.ifBlank { "Me" }
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
}
