package com.truckerload.data.repository.social

import android.content.Context
import com.truckerload.R
import com.truckerload.data.community.CommunityInboxSync
import com.truckerload.data.community.CommunityRemoteClient
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.repository.toDomain
import com.truckerload.data.social.ContentModerator
import com.truckerload.data.social.RecommendationService
import com.truckerload.di.UserScope
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.MessageType
import com.truckerload.domain.social.ModerationCodes
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialIdentity
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
    private val chatMemberDao: ChatMemberDao,
    private val recommendations: RecommendationService,
    private val appContext: Context,
    private val isBlocked: suspend (String) -> Boolean,
    private val actorId: () -> String,
    private val remote: CommunityRemoteClient,
    private val inbox: CommunityInboxSync,
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
        val me = actorId()
        val older = messageDao.getMessagesBefore(chatId, beforeSentAt, limit)
            .map { it.toDomain(isMine = SocialIdentity.isMine(it.senderId, me)) }
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
        if (trimmed.isNotEmpty()) {
            val moderation = ContentModerator.moderateText(trimmed)
            if (!moderation.allowed) {
                return SocialResult.Error(moderationMessage(appContext, moderation.reason))
            }
        }
        val now = System.currentTimeMillis()
        val preview = when (messageType) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VOICE -> "🎤 Voice"
            MessageType.ANNOUNCEMENT -> "📌 $trimmed"
            MessageType.TEXT -> trimmed
        }
        val messageId = UUID.randomUUID().toString()
        val body = trimmed.ifBlank { preview }
        if (remote.isReady()) {
            remote.sendMessage(
                id = messageId,
                chatId = chatId,
                senderName = senderName,
                text = body,
                messageType = messageType.name,
                attachmentUrl = attachmentUrl,
                replyToId = replyToId,
                locationLabel = locationLabel,
                durationMs = durationMs,
                sentAt = now,
            ).onFailure { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_send_message,
                        err
                    ), err
                )
            }
        }
        messageDao.insert(
            SocialMessageEntity(
                id = messageId,
                chatId = chatId,
                senderId = actorId(),
                senderName = senderName,
                text = body,
                sentAt = now,
                messageType = messageType.name,
                attachmentUrl = attachmentUrl,
                replyToId = replyToId,
                locationLabel = locationLabel,
                isAnnouncement = messageType == MessageType.ANNOUNCEMENT,
                durationMs = durationMs,
            ),
        )
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
                userId = actorId(),
                reaction = reaction,
                reactedAt = System.currentTimeMillis(),
            ),
        )
        if (remote.isReady()) {
            remote.addReaction(messageId, reaction)
        }
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
        val me = actorId()
        if (peerId == me) {
            return SocialResult.Error(appContext.getString(R.string.social_error_cannot_message_self))
        }
        if (isBlocked(peerId)) {
            return SocialResult.Error(appContext.getString(R.string.social_user_blocked))
        }
        if (remote.isReady()) {
            val remoteId = remote.createDm(peerId).getOrElse { err ->
                return SocialResult.Error(
                    socialError(
                        appContext,
                        R.string.social_error_create_chat,
                        err
                    ), err
                )
            }
            inbox.pullChats()
            return SocialResult.Success(remoteId)
        }
        findPrivateChatForPeer(peerId)?.let { return SocialResult.Success(it.id) }
        val peer = peerDao.getById(peerId)
            ?: return SocialResult.Error(appContext.getString(R.string.social_peer_not_found))
        val chatId = SocialIdentity.privateChatIdForPeer(peerId)
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
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = me,
                displayName = "You",
                role = "MEMBER",
                joinedAt = now
            ),
        )
        chatMemberDao.upsert(
            ChatMemberEntity(
                chatId = chatId,
                userId = peerId,
                displayName = peer.displayName,
                role = "MEMBER",
                joinedAt = now,
            ),
        )
        SocialResult.Success(chatId)
    }.getOrElse { SocialResult.Error(socialError(appContext, R.string.social_error_create_chat, it), it) }

    override suspend fun archivePrivateChatForPeer(peerId: String) {
        findPrivateChatForPeer(peerId)?.let { chatDao.archiveChat(it.id) }
    }

    override suspend fun privatePeerId(chatId: String): String? {
        SocialIdentity.peerIdFromPrivateChat(chatId)?.let { return it }
        return chatMemberDao.otherMemberId(chatId, actorId())
    }

    private suspend fun findPrivateChatForPeer(peerId: String): SocialChatEntity? {
        chatDao.getChat(SocialIdentity.privateChatIdForPeer(peerId))?.let { return it }
        val peer = peerDao.getById(peerId) ?: return null
        return findPrivateChatByTitle(peer.displayName)
    }

    private suspend fun findPrivateChatByTitle(title: String): SocialChatEntity? =
        chatDao.watchChats().first()
            .firstOrNull { it.type == ChatType.PRIVATE.name && it.title.equals(title, ignoreCase = true) }
}

private fun moderationMessage(context: Context, code: String?): String {
    val res = when (code) {
        ModerationCodes.PII_EMAIL, ModerationCodes.PII_PHONE -> R.string.social_error_hide_pii
        ModerationCodes.LINK, ModerationCodes.OFF_APP -> R.string.social_error_keep_in_app
        ModerationCodes.EMPTY -> R.string.social_error_empty_message
        else -> R.string.social_error_message_rejected
    }
    return context.getString(res)
}
