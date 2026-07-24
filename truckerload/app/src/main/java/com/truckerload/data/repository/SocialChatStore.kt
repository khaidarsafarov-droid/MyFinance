package com.truckerload.data.repository

import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.ReactionSummary
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class SocialChatStore(
    private val chatDao: SocialChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val blockedUserDao: BlockedUserDao,
    private val peerDao: SocialPeerDao,
    private val messageDao: SocialMessageDao,
    private val reactionDao: MessageReactionDao,
) {
    fun watchChats(): Flow<List<SocialChat>> =
        combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds, blockedIds ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet())
        }.flowOn(Dispatchers.IO)

    fun watchPublicGroups(): Flow<List<SocialChat>> =
        combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds ->
            val memberSet = memberChatIds.toSet()
            chats
                .filter { it.type == ChatType.GROUP.name && it.isPublic && !it.archived }
                .map { it.toDomain(isMember = memberSet.contains(it.id)) }
        }.flowOn(Dispatchers.IO)

    fun watchPeers(): Flow<List<SocialPeerProfile>> =
        combine(
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            peers.filter { it.id !in blockedSet }.map { it.toPeerProfile() }
        }.flowOn(Dispatchers.IO)

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> {
        val trimmed = query.trim()
        val chatSource = if (trimmed.isEmpty()) {
            chatDao.watchChats()
        } else {
            chatDao.watchChatsSearch(trimmed)
        }
        return combine(
            chatSource,
            chatMemberDao.watchMemberChatIds(DriverProfileEntity.LOCAL_USER_ID),
            blockedUserDao.watchBlockedIds(DriverProfileEntity.LOCAL_USER_ID),
        ) { chats, memberChatIds, blockedIds ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet())
        }.flowOn(Dispatchers.IO)
    }

    fun watchTotalUnread(): Flow<Int> = chatDao.watchTotalUnread().flowOn(Dispatchers.IO)

    fun watchMessages(chatId: String, limit: Int): Flow<List<SocialMessage>> =
        combine(
            messageDao.watchRecentMessages(chatId, limit),
            reactionDao.watchReactionsForChat(chatId),
        ) { messages, reactions ->
            val byId = messages.associateBy { it.id }
            messages.map { entity ->
                entity.toDomain(
                    isMine = entity.senderId == LOCAL_SENDER_ID,
                    reactions = summarizeReactions(reactions.filter { it.messageId == entity.id }),
                    replyPreview = entity.replyToId?.let { byId[it]?.text },
                )
            }
        }.flowOn(Dispatchers.IO)

    private fun mapChatsWithMembership(
        chats: List<SocialChatEntity>,
        memberChatIds: Set<String>,
        blockedIds: Set<String>,
    ): List<SocialChat> =
        chats
            .filter { chat -> chat.type != ChatType.PRIVATE.name || !isBlockedPrivateChat(chat, blockedIds) }
            .map { chat -> chat.toDomain(isMember = resolveIsMember(chat, memberChatIds)) }

    private fun resolveIsMember(chat: SocialChatEntity, memberChatIds: Set<String>): Boolean =
        when {
            chat.type == ChatType.PRIVATE.name -> true
            chat.type == ChatType.GROUP.name -> memberChatIds.contains(chat.id)
            else -> true
        }

    private fun isBlockedPrivateChat(chat: SocialChatEntity, blockedIds: Set<String>): Boolean {
        if (chat.type != ChatType.PRIVATE.name) return false
        val peerId = chat.id.removePrefix("dm_")
        return peerId.startsWith("peer_") && peerId in blockedIds
    }

    private fun summarizeReactions(reactions: List<MessageReactionEntity>): List<ReactionSummary> =
        reactions.groupBy { it.reaction }.map { (emoji, list) ->
            ReactionSummary(
                reaction = emoji,
                count = list.size,
                includesMe = list.any { it.userId == DriverProfileEntity.LOCAL_USER_ID },
            )
        }

    private companion object {
        const val LOCAL_SENDER_ID = "me"
    }
}
