package com.truckerload.domain.social

data class DriverProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val truckType: String,
    val experienceYears: Int,
    val homeState: String,
    val routes: List<String>,
    val rating: Double,
    val totalLoads: Int,
    val totalMiles: Int,
    val totalRevenue: Double,
    val status: DriverStatus,
    val about: String,
    val badges: List<Badge>,
    val joinedDate: Long,
)

enum class DriverStatus(val label: String) {
    ONLINE("🟢 В сети"),
    ON_ROAD("🛣️ В рейсе"),
    RESTING("😴 Отдыхает"),
    OFFLINE("⚫ Не в сети"),
}

data class Badge(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val unlockedAt: Long,
)

enum class ChatType { PRIVATE, GROUP, CHANNEL }

enum class MessageType { TEXT, IMAGE, VOICE, ANNOUNCEMENT }

data class SocialChat(
    val id: String,
    val title: String,
    val type: ChatType,
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
    val isMember: Boolean = false,
)

data class SocialMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAt: Long,
    val messageType: MessageType = MessageType.TEXT,
    val attachmentUrl: String? = null,
    val isMine: Boolean = false,
    val replyToId: String? = null,
    val replyPreview: String? = null,
    val locationLabel: String? = null,
    val isAnnouncement: Boolean = false,
    val reactions: List<ReactionSummary> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val durationMs: Long = 0,
)

data class ChatMember(
    val chatId: String,
    val userId: String,
    val displayName: String,
    val role: String,
    val joinedAt: Long,
    val isMe: Boolean = false,
)

enum class ChallengeType(val label: String) {
    MILES("📏 Больше всех миль"),
    REVENUE("💰 Больше всех дохода"),
    RPM("📈 Лучший RPM"),
    LOADS("📦 Больше всех грузов"),
    PUNCTUALITY("⏱️ Точность"),
}

data class Challenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val goal: Double,
    val startDate: Long,
    val endDate: Long,
    val leaderboard: List<LeaderboardEntry>,
    val myPosition: Int?,
    val myScore: Double,
)

data class LeaderboardEntry(
    val rank: Int,
    val displayName: String,
    val score: Double,
    val rating: Double,
    val trend: String,
    val isMe: Boolean = false,
    val userId: String? = null,
)

data class SocialPeerProfile(
    val id: String,
    val displayName: String,
    val rating: Double,
    val weeklyMiles: Double,
    val weeklyRevenue: Double,
    val weeklyLoads: Int,
    val weeklyRpm: Double,
)

data class DriverStatusPost(
    val id: String,
    val userId: String,
    val displayName: String,
    val type: StatusType,
    val text: String?,
    val mediaPath: String?,
    val createdAt: Long,
    val expiresAt: Long,
    val viewed: Boolean,
    val durationMs: Long = 0,
)

enum class StatusType { TEXT, PHOTO, VOICE }
