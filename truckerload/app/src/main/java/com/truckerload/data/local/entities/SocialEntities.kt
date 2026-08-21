package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "social_chats",
    indices = [
        Index(value = ["lastMessageAt"]),
        Index(value = ["title"]),
        Index(value = ["archived", "lastMessageAt"]),
        Index(value = ["inviteCode"]),
    ],
)
data class SocialChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val participantCount: Int,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unreadCount: Int,
    val avatarEmoji: String,
    val onlineCount: Int = 0,
    val category: String = "",
    val archived: Boolean = false,
    val description: String = "",
    val rating: Double = 4.5,
    val isPublic: Boolean = true,
    val creatorId: String = "",
    val inviteCode: String = "",
)

@Entity(
    tableName = "social_messages",
    indices = [Index(value = ["chatId"]), Index(value = ["chatId", "sentAt"])],
)
data class SocialMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
    val messageType: String = "TEXT",
    val attachmentUrl: String? = null,
    val replyToId: String? = null,
    val locationLabel: String? = null,
    val isAnnouncement: Boolean = false,
    val durationMs: Long = 0,
)

@Entity(
    tableName = "message_reactions",
    primaryKeys = ["messageId", "userId", "reaction"],
    indices = [Index(value = ["messageId"])],
)
data class MessageReactionEntity(
    val messageId: String,
    val userId: String,
    val reaction: String,
    val reactedAt: Long,
)

@Entity(tableName = "blocked_users", primaryKeys = ["blockerId", "blockedId"])
data class BlockedUserEntity(
    val blockerId: String,
    val blockedId: String,
    val blockedAt: Long,
)

@Entity(
    tableName = "driver_statuses",
    indices = [Index(value = ["expiresAt"]), Index(value = ["userId"])],
)
data class DriverStatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val type: String,
    val text: String?,
    val mediaPath: String?,
    val createdAt: Long,
    val expiresAt: Long,
    val viewed: Boolean = false,
    val durationMs: Long = 0,
)

@Entity(
    tableName = "driver_follows",
    primaryKeys = ["followerId", "followingId"],
    indices = [Index(value = ["followingId"])],
)
data class DriverFollowEntity(
    val followerId: String,
    val followingId: String,
    val followedAt: Long,
)

@Entity(
    tableName = "chat_members",
    primaryKeys = ["chatId", "userId"],
    indices = [Index(value = ["chatId"]), Index(value = ["userId"])],
)
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    val displayName: String,
    val role: String = "MEMBER",
    val joinedAt: Long,
)

@Entity(
    tableName = "social_peers",
    indices = [Index(value = ["weeklyMiles"])],
)
data class SocialPeerEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val rating: Double,
    val weeklyMiles: Double,
    val weeklyRevenue: Double,
    val weeklyLoads: Int,
    val weeklyRpm: Double,
)

@Entity(
    tableName = "challenge_participation",
    primaryKeys = ["challengeId", "userId"],
    indices = [Index(value = ["challengeId", "score"])],
)
data class ChallengeParticipationEntity(
    val challengeId: String,
    val userId: String,
    val score: Double,
    val joinedAt: Long,
)
