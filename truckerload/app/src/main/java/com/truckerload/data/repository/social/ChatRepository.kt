package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.repository.SocialChatStore
import com.truckerload.data.repository.toDomain
import com.truckerload.data.social.ContentModerator
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

/** Direct messages, threads, reactions, and unread state. */
class ChatRepository(
    private val chatDao: SocialChatDao,
    private val messageDao: SocialMessageDao,
    private val reactionDao: MessageReactionDao,
    private val blockedUserDao: BlockedUserDao,
    private val peerDao: SocialPeerDao,
    private val chatStore: SocialChatStore,
    context: Context,
) {
    private val appContext = context.applicationContext

    fun watchChats(): Flow<List<SocialChat>> = chatStore.watchChats().flowOn(Dispatchers.IO)

    fun watchPeers(): Flow<List<SocialPeerProfile>> = chatStore.watchPeers().flowOn(Dispatchers.IO)

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> =
        chatStore.watchChatsSearch(query).flowOn(Dispatchers.IO)

    fun watchTotalUnread(): Flow<Int> = chatStore.watchTotalUnread().flowOn(Dispatchers.IO)

    fun watchMessages(
        chatId: String,
        limit: Int = SocialConstants.MESSAGE_PAGE_SIZE,
    ): Flow<List<SocialMessage>> =
        chatStore.watchMessages(chatId, limit).flowOn(Dispatchers.IO)

    suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int = SocialConstants.MESSAGE_PAGE_SIZE,
    ): SocialResult<List<SocialMessage>> = runCatching {
        val older = messageDao.getMessagesBefore(chatId, beforeSentAt, limit)
            .map { it.toDomain(isMine = it.senderId == SocialConstants.LOCAL_SENDER_ID) }
        SocialResult.Success(older)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_load_messages, it), it) }

    suspend fun getChat(chatId: String): SocialChat? =
        chatDao.getChat(chatId)?.toDomain()

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        replyToId: String? = null,
        locationLabel: String? = null,
        durationMs: Long = 0,
    ): SocialResult<Unit> = runCatching {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachmentUrl.isNullOrBlank()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_empty_message))
        }
        val moderation = ContentModerator.moderateText(trimmed)
        if (!moderation.allowed) {
            return SocialResult.Error(
                moderation.reason ?: appContext.getString(R.string.social_error_message_rejected),
            )
        }
        val now = System.currentTimeMillis()
        val preview = when (messageType) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VOICE -> "🎤 Voice"
            MessageType.ANNOUNCEMENT -> "📌 $trimmed"
            MessageType.TEXT -> trimmed
        }
        val message = SocialMessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = SocialConstants.LOCAL_SENDER_ID,
            senderName = senderName,
            text = trimmed.ifBlank { preview },
            sentAt = now,
            messageType = messageType.name,
            attachmentUrl = attachmentUrl,
            replyToId = replyToId,
            locationLabel = locationLabel,
            isAnnouncement = messageType == MessageType.ANNOUNCEMENT,
            durationMs = durationMs,
        )
        messageDao.insert(message)
        val chat = chatDao.getChat(chatId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_error_chat_not_found))
        chatDao.upsert(
            chat.copy(
                lastMessage = preview,
                lastMessageAt = now,
                unreadCount = 0,
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_send_message, it), it) }

    suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit> = runCatching {
        reactionDao.upsert(
            MessageReactionEntity(
                messageId = messageId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                reaction = reaction,
                reactedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_add_reaction, it), it) }

    suspend fun markChatRead(chatId: String) {
        chatDao.markRead(chatId)
    }

    suspend fun createPrivateChat(peerName: String): SocialResult<String> = runCatching {
        val trimmedName = peerName.trim()
        if (trimmedName.isBlank()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_create_chat))
        }
        chatDao.findPrivateChatByTitle(trimmedName)?.let { return SocialResult.Success(it.id) }
        val chatId = "dm_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = trimmedName,
                type = ChatType.PRIVATE.name,
                participantCount = 2,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👤",
                onlineCount = 0,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_create_chat, it), it) }

    suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String> = runCatching {
        if (peerId == DriverProfileEntity.LOCAL_USER_ID) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_message_self))
        }
        if (blockedUserDao.isBlocked(DriverProfileEntity.LOCAL_USER_ID, peerId)) {
            return SocialResult.Error(appContext.getString(R.string.social_user_blocked))
        }
        chatDao.findPrivateChatForPeer(peerDao, peerId)?.let { return SocialResult.Success(it.id) }
        val peer = peerDao.getById(peerId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_peer_not_found))
        val chatId = privateChatIdForPeer(peerId)
        val now = System.currentTimeMillis()
        chatDao.upsert(
            SocialChatEntity(
                id = chatId,
                title = peer.displayName,
                type = ChatType.PRIVATE.name,
                participantCount = 2,
                lastMessage = "",
                lastMessageAt = now,
                unreadCount = 0,
                avatarEmoji = "👤",
                onlineCount = 1,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(appContext.socialError(R.string.social_error_create_chat, it), it) }
}
