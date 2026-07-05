package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "driver_profile")
data class DriverProfileEntity(
    @PrimaryKey val id: String = LOCAL_USER_ID,
    val displayName: String = "",
    val avatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val truckType: String = "",
    val experienceYears: Int = 0,
    val licenseClass: String = "A",
    val endorsementsJson: String = "",
    val homeState: String = "",
    val routesJson: String = "",
    val maxRadius: Int = 500,
    val about: String = "",
    val specialtiesJson: String = "",
    val languagesJson: String = "Русский,Английский",
    val phoneNumber: String? = null,
    val telegramUsername: String? = null,
    val whatsappNumber: String? = null,
    val reputation: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val ratingCount: Int = 124,
    val currentRoute: String? = null,
    val status: String = "OFFLINE",
    val joinedDate: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
) {
    companion object {
        const val LOCAL_USER_ID = "local_user"
    }
}

@Entity(
    tableName = "social_chats",
    indices = [Index(value = ["lastMessageAt"]), Index(value = ["title"])],
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

@Entity(tableName = "driver_follows", primaryKeys = ["followerId", "followingId"])
data class DriverFollowEntity(
    val followerId: String,
    val followingId: String,
    val followedAt: Long,
)

@Entity(
    tableName = "chat_members",
    primaryKeys = ["chatId", "userId"],
    indices = [Index(value = ["chatId"])],
)
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    val displayName: String,
    val role: String = "MEMBER",
    val joinedAt: Long,
)

@Entity(tableName = "social_peers")
data class SocialPeerEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val rating: Double,
    val weeklyMiles: Double,
    val weeklyRevenue: Double,
    val weeklyLoads: Int,
    val weeklyRpm: Double,
)

@Entity(tableName = "challenge_participation", primaryKeys = ["challengeId", "userId"])
data class ChallengeParticipationEntity(
    val challengeId: String,
    val userId: String,
    val score: Double,
    val joinedAt: Long,
)
