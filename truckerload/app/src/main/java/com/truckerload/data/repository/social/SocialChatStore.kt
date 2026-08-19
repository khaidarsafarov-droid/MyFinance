package com.truckerload.data.repository.social

import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.repository.toDomain
import com.truckerload.data.repository.toPeerProfile
import com.truckerload.domain.social.ChatType
import com.truckerload.domain.social.ReactionSummary
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialIdentity
import com.truckerload.domain.social.SocialMessage
import com.truckerload.domain.social.SocialPeerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

class SocialChatStore(
    private val chatDao: SocialChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val blockedUserDao: BlockedUserDao,
    private val peerDao: SocialPeerDao,
    private val messageDao: SocialMessageDao,
    private val reactionDao: MessageReactionDao,
    private val actorId: () -> String,
) {
    fun watchChats(): Flow<List<SocialChat>> {
        val me = actorId()
        return combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(me),
            blockedUserDao.watchBlockedIds(me),
            chatMemberDao.watchAll(),
        ) { chats, memberChatIds, blockedIds, members ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet(), members, me)
        }.flowOn(Dispatchers.IO)
    }

    fun watchPublicGroups(): Flow<List<SocialChat>> {
        val me = actorId()
        return combine(
            chatDao.watchChats(),
            chatMemberDao.watchMemberChatIds(me),
        ) { chats, memberChatIds ->
            val memberSet = memberChatIds.toSet()
            chats
                .filter { it.type == ChatType.GROUP.name && it.isPublic && !it.archived }
                .map { it.toDomain(isMember = memberSet.contains(it.id)) }
        }.flowOn(Dispatchers.IO)
    }

    fun watchPeers(): Flow<List<SocialPeerProfile>> {
        val me = actorId()
        return combine(
            peerDao.watchAll(),
            blockedUserDao.watchBlockedIds(me),
        ) { peers, blockedIds ->
            val blockedSet = blockedIds.toSet()
            peers.filter { it.id !in blockedSet }.map { it.toPeerProfile() }
        }.flowOn(Dispatchers.IO)
    }

    fun watchChatsSearch(query: String): Flow<List<SocialChat>> {
        val trimmed = query.trim()
        val chatSource = if (trimmed.isEmpty()) {
            chatDao.watchChats()
        } else {
            chatDao.watchChatsSearch(trimmed)
        }
        val me = actorId()
        return combine(
            chatSource,
            chatMemberDao.watchMemberChatIds(me),
            blockedUserDao.watchBlockedIds(me),
            chatMemberDao.watchAll(),
        ) { chats, memberChatIds, blockedIds, members ->
            mapChatsWithMembership(chats, memberChatIds.toSet(), blockedIds.toSet(), members, me)
        }.flowOn(Dispatchers.IO)
    }

    fun watchTotalUnread(): Flow<Int> = chatDao.watchTotalUnread().flowOn(Dispatchers.IO)

    fun watchMessages(chatId: String, limit: Int): Flow<List<SocialMessage>> {
        val me = actorId()
        return combine(
            messageDao.watchRecentMessages(chatId, limit),
            reactionDao.watchReactionsForChat(chatId),
        ) { messages, reactions ->
            val byId = messages.associateBy { it.id }
            messages
                .sortedWith(compareBy({ it.sentAt }, { it.id }))
                .map { entity ->
                entity.toDomain(
                    isMine = SocialIdentity.isMine(entity.senderId, me),
                    reactions = summarizeReactions(
                        reactions.filter { it.messageId == entity.id },
                        me
                    ),
                    replyPreview = entity.replyToId?.let { byId[it]?.text },
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun mapChatsWithMembership(
        chats: List<SocialChatEntity>,
        memberChatIds: Set<String>,
        blockedIds: Set<String>,
        members: List<com.truckerload.data.local.entities.ChatMemberEntity>,
        me: String,
    ): List<SocialChat> {
        val membersByChat = members.groupBy { it.chatId }
        return chats
            .filter { chat ->
                chat.type != ChatType.PRIVATE.name ||
                        !isBlockedPrivateChat(
                            chat,
                            blockedIds,
                            membersByChat[chat.id].orEmpty(),
                            me
                        )
            }
            .map { chat -> chat.toDomain(isMember = resolveIsMember(chat, memberChatIds)) }
    }

    private fun resolveIsMember(chat: SocialChatEntity, memberChatIds: Set<String>): Boolean =
        when {
            chat.type == ChatType.PRIVATE.name -> true
            chat.type == ChatType.GROUP.name -> memberChatIds.contains(chat.id)
            else -> true
        }

    private fun isBlockedPrivateChat(
        chat: SocialChatEntity,
        blockedIds: Set<String>,
        members: List<com.truckerload.data.local.entities.ChatMemberEntity>,
        me: String,
    ): Boolean {
        if (chat.type != ChatType.PRIVATE.name) return false
        if (members.any { it.userId != me && it.userId in blockedIds }) return true
        val peerId = SocialIdentity.peerIdFromPrivateChat(chat.id)
        return peerId != null && peerId in blockedIds
    }

    private fun summarizeReactions(
        reactions: List<MessageReactionEntity>,
        me: String
    ): List<ReactionSummary> =
        reactions.groupBy { it.reaction }.map { (emoji, list) ->
            ReactionSummary(
                reaction = emoji,
                count = list.size,
                includesMe = list.any { SocialIdentity.isMine(it.userId, me) },
            )
        }
}
