package com.truckerload.data.community

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.domain.social.ChatType
import java.io.File

class CommunityInboxSync(
    private val db: AppDatabase,
    private val remote: CommunityRemoteClient,
    private val storage: CommunityStorageClient,
    private val cacheDir: File,
) {
    suspend fun pullAll() {
        if (!remote.isReady()) return
        pullPeers()
        pullChats()
        pullMessages()
        pullStatuses()
    }

    suspend fun pullPeers() {
        val peers = remote.listPeers().getOrElse { return }
        val dao = db.socialPeerDao()
        dao.deleteAll()
        if (peers.isNotEmpty()) {
            dao.upsertAll(
                peers.map { peer ->
                    SocialPeerEntity(
                        id = peer.userId,
                        displayName = peer.displayName,
                        rating = 0.0,
                        weeklyMiles = peer.weeklyMiles,
                        weeklyRevenue = peer.weeklyRevenue,
                        weeklyLoads = peer.weeklyLoads,
                        weeklyRpm = peer.weeklyRpm,
                    )
                },
            )
        }
    }

    suspend fun pullChats() {
        val chats = remote.listChats().getOrElse { return }
        val members = remote.listMembers().getOrElse { emptyList() }
        val memberCount = members.groupingBy { it.chatId }.eachCount()
        chats.forEach { chat ->
            val existing = db.socialChatDao().getChat(chat.id)
            db.socialChatDao().upsert(
                SocialChatEntity(
                    id = chat.id,
                    title = chat.title,
                    type = chat.type.ifBlank { ChatType.GROUP.name },
                    participantCount = memberCount[chat.id] ?: existing?.participantCount ?: 1,
                    lastMessage = chat.lastMessage.ifBlank { existing?.lastMessage.orEmpty() },
                    lastMessageAt = chat.lastMessageAt.takeIf { it > 0 } ?: existing?.lastMessageAt
                    ?: System.currentTimeMillis(),
                    unreadCount = existing?.unreadCount ?: 0,
                    avatarEmoji = if (chat.type == ChatType.PRIVATE.name) "👤" else "👥",
                    onlineCount = existing?.onlineCount ?: 0,
                    category = chat.category,
                    archived = existing?.archived ?: false,
                    description = existing?.description.orEmpty(),
                    rating = existing?.rating ?: 0.0,
                    isPublic = chat.isPublic,
                    creatorId = chat.creatorId,
                    inviteCode = chat.inviteCode,
                ),
            )
        }
        members.forEach { member ->
            db.chatMemberDao().upsert(
                ChatMemberEntity(
                    chatId = member.chatId,
                    userId = member.userId,
                    displayName = member.displayName,
                    role = member.role,
                    joinedAt = member.joinedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun pullMessages() {
        val messages = remote.listMessages().getOrElse { return }
        val me = remote.actorId()
        messages.forEach { msg ->
            val existed = db.socialMessageDao().getMessage(msg.id) != null
            val localPath = cacheAttachment(msg.attachmentUrl)
            db.socialMessageDao().insert(
                SocialMessageEntity(
                    id = msg.id,
                    chatId = msg.chatId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    text = msg.text,
                    sentAt = msg.sentAt,
                    messageType = msg.messageType,
                    attachmentUrl = localPath ?: msg.attachmentUrl,
                    replyToId = msg.replyToId,
                    locationLabel = msg.locationLabel,
                    durationMs = msg.durationMs,
                ),
            )
            if (!existed && msg.senderId != me) {
                db.socialChatDao().incrementUnread(msg.chatId)
            }
        }
    }

    suspend fun pullStatuses() {
        val statuses = remote.listStatuses().getOrElse { return }
        statuses.forEach { status ->
            val localPath = cacheAttachment(status.mediaPath)
            db.driverStatusDao().insert(
                DriverStatusEntity(
                    id = status.id,
                    userId = status.userId,
                    displayName = status.displayName,
                    type = status.type,
                    text = status.text,
                    mediaPath = localPath ?: status.mediaPath,
                    createdAt = status.createdAt,
                    expiresAt = status.expiresAt,
                    durationMs = status.durationMs,
                ),
            )
        }
    }

    private suspend fun cacheAttachment(remotePath: String?): String? {
        if (remotePath.isNullOrBlank()) return null
        if (remotePath.startsWith("/") || File(remotePath).isAbsolute) return remotePath
        if (remotePath.startsWith("http://") || remotePath.startsWith("https://")) return remotePath
        val dest = File(cacheDir, remotePath.replace('/', '_'))
        if (dest.exists() && dest.length() > 0) return dest.absolutePath
        return storage.download(remotePath, dest).getOrNull()?.absolutePath
    }
}
