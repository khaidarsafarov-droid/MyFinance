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
    val phoneNumber: String?,
    val telegramUsername: String?,
    val whatsappNumber: String?,
    val joinedDate: Long,
    val lastActive: Long,
)

enum class TruckType(val label: String, val emoji: String) {
    FLATBED("Flatbed", "📦"),
    REEFER("Reefer", "❄️"),
    DRY_VAN("Dry Van", "🚛"),
    TANKER("Tanker", "🛢️"),
    OTHER("Другое", "🛣️"),
    ;

    companion object {
        fun fromLabel(value: String): TruckType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: DRY_VAN
    }
}

enum class BadgeType(val icon: String, val title: String, val description: String) {
    LOAD_MASTER("🏆", "Мастер грузов", "1000+ грузов"),
    MILE_KING("👑", "Король миль", "100 000+ миль"),
    RPM_CHAMPION("⭐", "Чемпион RPM", "$2.50+/миля"),
    FLATBED_SPECIALIST("📦", "Flatbed", "500+ грузов на платформе"),
    REEFER_SPECIALIST("❄️", "Reefer", "500+ грузов с охлаждением"),
    HAZMAT_SPECIALIST("☣️", "Hazmat", "Сертификат Hazmat"),
    HELPER("🤝", "Помощник", "Помог 50+ водителям"),
    MENTOR("🎓", "Наставник", "Обучил 10+ водителей"),
    COMMUNITY_LEADER("🌟", "Лидер сообщества", "Создал 5+ групп"),
    PUNCTUAL("⏱️", "Пунктуальный", "95%+ вовремя"),
    RELIABLE("🔒", "Надёжный", "Никогда не отменял груз"),
    LEGEND("🏅", "Легенда", "10 лет в профессии"),
    FIRST_LOAD("🎯", "Первый груз", "Первый загруженный груз"),
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
    OVERALL("📊 Общий"),
    LOADS("📦 Грузы"),
    REVENUE("💰 Доход"),
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
