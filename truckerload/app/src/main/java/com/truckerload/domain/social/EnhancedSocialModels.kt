package com.truckerload.domain.social

data class EnhancedDriverProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val coverImageUrl: String?,
    val truckType: TruckType,
    val experienceYears: Int,
    val licenseClass: String,
    val endorsements: List<String>,
    val homeState: String,
    val preferredRoutes: List<String>,
    val maxRadius: Int,
    val totalLoads: Int,
    val totalMiles: Int,
    val totalRevenue: Double,
    val averageRpm: Double,
    val onTimePercentage: Double,
    val rating: Double,
    val ratingCount: Int,
    val reputation: Int,
    val badges: List<Badge>,
    val followers: Int,
    val following: Int,
    val status: DriverStatus,
    val currentRoute: String?,
    val about: String,
    val specialties: List<String>,
    val languages: List<String>,
    /** Account phone kept for the edit form and registration, not shown on the profile. */
    val phoneNumber: String?,
    val joinedDate: Long,
    val lastActive: Long,
    val dateOfBirthEpochDay: Long? = null,
    val axleCount: Int = 0,
    val homeHubCity: String = "",
)

enum class TruckType(val label: String, val emoji: String) {
    FLATBED("Flatbed", "📦"),
    REEFER("Reefer", "❄️"),
    DRY_VAN("Dry Van", "🚛"),
    TANKER("Tanker", "🛢️"),
    OTHER("Other", "🛣️"),
    ;

    companion object {
        fun fromLabel(value: String): TruckType =
            if (value.isBlank()) OTHER
            else entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: OTHER
    }
}

enum class BadgeType(val icon: String, val title: String, val description: String) {
    LOAD_MASTER("🏆", "Load Master", "1000+ loads"),
    MILE_KING("👑", "Mile King", "100,000+ miles"),
    RPM_CHAMPION("⭐", "RPM Champion", "$2.50+/mile"),
    FLATBED_SPECIALIST("📦", "Flatbed", "500+ flatbed loads"),
    REEFER_SPECIALIST("❄️", "Reefer", "500+ reefer loads"),
    HAZMAT_SPECIALIST("☣️", "Hazmat", "Hazmat certified"),
    HELPER("🤝", "Helper", "Helped 50+ drivers"),
    MENTOR("🎓", "Mentor", "Mentored 10+ drivers"),
    COMMUNITY_LEADER("🌟", "Community Leader", "Created 5+ groups"),
    PUNCTUAL("⏱️", "Punctual", "95%+ on time"),
    RELIABLE("🔒", "Reliable", "Never canceled a load"),
    LEGEND("🏅", "Legend", "10 years on the road"),
    FIRST_LOAD("🎯", "First Load", "First load logged"),
}

enum class EnhancedChatType {
    DIRECT,
    GROUP,
    VOICE_CHANNEL,
    ANNOUNCEMENT,
    HELP,
    MARKETPLACE,
    ROUTE_SHARING,
    ;

    companion object {
        fun fromLegacy(type: ChatType): EnhancedChatType = when (type) {
            ChatType.PRIVATE -> DIRECT
            ChatType.GROUP -> GROUP
            ChatType.CHANNEL -> ANNOUNCEMENT
        }
    }
}

enum class ReactionEmoji(val emoji: String) {
    THUMBS_UP("👍"),
    HEART("❤️"),
    LAUGH("😂"),
    WOW("😮"),
    SAD("😢"),
    ANGRY("😡"),
}

data class MessageReaction(
    val messageId: String,
    val userId: String,
    val reaction: String,
)

data class ReactionSummary(
    val reaction: String,
    val count: Int,
    val includesMe: Boolean,
)

enum class LeaderboardCategory(val label: String) {
    OVERALL("📊 Overall"),
    LOADS("📦 Loads"),
    REVENUE("💰 Revenue"),
    RPM("📈 RPM"),
}

enum class NotificationType {
    NEW_MESSAGE,
    NEW_REPLY,
    MENTION,
    REACTION,
    INCOMING_CALL,
    MISSED_CALL,
    NEW_STATUS,
    NEW_FOLLOWER,
    CHALLENGE_START,
    CHALLENGE_UPDATE,
    BADGE_UNLOCKED,
    GROUP_INVITE,
}

enum class ContentType {
    MESSAGE, STATUS, COMMENT, PROFILE, GROUP
}

data class ModerationResult(
    val allowed: Boolean,
    val reason: String? = null,
)
