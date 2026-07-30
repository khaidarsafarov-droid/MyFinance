package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.repository.toDomain
import com.truckerload.data.social.ContentModerator
import com.truckerload.data.social.RecommendationService
import com.truckerload.di.UserScope
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import com.truckerload.domain.social.SocialResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

@UserScope
class ChatRepositoryImpl(
    private val chatStore: SocialChatStore,
    private val chatDao: SocialChatDao,
    private val messageDao: SocialMessageDao,
    private val reactionDao: MessageReactionDao,
    private val peerDao: SocialPeerDao,
    private val recommendations: RecommendationService,
    private val appContext: Context,
    private val isBlocked: suspend (String) -> Boolean,
) : ChatRepository {

    override fun watchChats(): Flow<List<SocialChat>> = chatStore.watchChats().flowOn(Dispatchers.IO)

    override fun watchPublicGroups(): Flow<List<SocialChat>> = chatStore.watchPublicGroups().flowOn(Dispatchers.IO)

    override fun watchPeers(): Flow<List<SocialPeerProfile>> = chatStore.watchPeers().flowOn(Dispatchers.IO)

    override fun watchChatsSearch(query: String): Flow<List<SocialChat>> =
        chatStore.watchChatsSearch(query).flowOn(Dispatchers.IO)

    override fun watchTotalUnread(): Flow<Int> = chatStore.watchTotalUnread().flowOn(Dispatchers.IO)

    override fun watchMessages(chatId: String, limit: Int): Flow<List<SocialMessage>> =
        chatStore.watchMessages(chatId, limit).flowOn(Dispatchers.IO)

    override suspend fun loadMoreMessages(
        chatId: String,
        beforeSentAt: Long,
        limit: Int,
    ): SocialResult<List<SocialMessage>> = runCatching {
        val older = messageDao.getMessagesBefore(chatId, beforeSentAt, limit)
            .map { it.toDomain(isMine = it.senderId == SocialConstants.LOCAL_SENDER_ID) }
        SocialResult.Success(older)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_load_messages, it), it) }

    override suspend fun getChat(chatId: String): SocialChat? =
        chatDao.getChat(chatId)?.toDomain()

    override suspend fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        messageType: MessageType,
        attachmentUrl: String?,
        replyToId: String?,
        locationLabel: String?,
        durationMs: Long,
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
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_send_message, it), it) }

    override suspend fun addReaction(messageId: String, reaction: String): SocialResult<Unit> = runCatching {
        reactionDao.upsert(
            MessageReactionEntity(
                messageId = messageId,
                userId = DriverProfileEntity.LOCAL_USER_ID,
                reaction = reaction,
                reactedAt = System.currentTimeMillis(),
            ),
        )
        SocialResult.Success(Unit)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_add_reaction, it), it) }

    override fun recommendGroups(): Flow<List<SocialChat>> =
        watchChats().map { recommendations.recommendGroups(it) }.flowOn(Dispatchers.IO)

    override suspend fun markChatRead(chatId: String) {
        chatDao.markRead(chatId)
    }

    override suspend fun createPrivateChat(peerName: String): SocialResult<String> = runCatching {
        val trimmedName = peerName.trim()
        if (trimmedName.isBlank()) {
            return SocialResult.Error(appContext.getString(R.string.social_error_create_chat))
        }
        findPrivateChatByTitle(trimmedName)?.let { return SocialResult.Success(it.id) }
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
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_chat, it), it) }

    override suspend fun createPrivateChatWithPeer(peerId: String): SocialResult<String> = runCatching {
        if (peerId == DriverProfileEntity.LOCAL_USER_ID) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_message_self))
        }
        if (isBlocked(peerId)) {
            return SocialResult.Error(appContext.getString(R.string.social_user_blocked))
        }
        findPrivateChatForPeer(peerId)?.let { return SocialResult.Success(it.id) }
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
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_chat, it), it) }

    override suspend fun archivePrivateChatForPeer(peerId: String) {
        findPrivateChatForPeer(peerId)?.let { chatDao.archiveChat(it.id) }
    }

    private fun privateChatIdForPeer(peerId: String): String = "dm_$peerId"

    private suspend fun findPrivateChatForPeer(peerId: String): SocialChatEntity? {
        chatDao.getChat(privateChatIdForPeer(peerId))?.let { return it }
        val peer = peerDao.getById(peerId) ?: return null
        return findPrivateChatByTitle(peer.displayName)
    }

    private suspend fun findPrivateChatByTitle(title: String): SocialChatEntity? =
        chatDao.watchChats().first()
            .firstOrNull { it.type == ChatType.PRIVATE.name && it.title.equals(title, ignoreCase = true) }
}
