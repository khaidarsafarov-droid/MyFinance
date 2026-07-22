package com.truckerload.domain.social

object BadgeEngine {
    fun compute(
        totalLoads: Int,
        totalMiles: Int,
        totalRevenue: Double,
        averageRpm: Double,
        experienceYears: Int,
        endorsements: List<String>,
        onTimePercentage: Double,
    ): List<Badge> {
        val now = System.currentTimeMillis()
        val badges = mutableListOf<Badge>()
        if (totalLoads >= 1) {
            badges.add(badge(BadgeType.FIRST_LOAD, now))
        }
        if (totalLoads >= 100) {
            badges.add(badge(BadgeType.LOAD_MASTER, now, "100 loads"))
        }
        if (totalLoads >= 1000) {
            badges.add(badge(BadgeType.LOAD_MASTER, now))
        }
        if (totalMiles >= 50_000) {
            badges.add(Badge("miles_50k", "Road Warrior", "🗺️", "50,000+ miles", now))
        }
        if (totalMiles >= 100_000) {
            badges.add(badge(BadgeType.MILE_KING, now))
        }
        if (averageRpm >= 2.5) {
            badges.add(badge(BadgeType.RPM_CHAMPION, now))
        }
        if (totalRevenue >= 100_000) {
            badges.add(Badge("revenue_100k", "Golden RPM", "⛽", "$100k+ gross", now))
        }
        if (totalLoads >= 500) {
            badges.add(badge(BadgeType.LEGEND, now, "500+ loads"))
        }
        if (experienceYears >= 10) {
            badges.add(badge(BadgeType.LEGEND, now))
        }
        if (endorsements.any { it.contains("Hazmat", ignoreCase = true) }) {
            badges.add(badge(BadgeType.HAZMAT_SPECIALIST, now))
        }
        if (onTimePercentage >= 95.0) {
            badges.add(badge(BadgeType.PUNCTUAL, now))
        }
        if (totalLoads >= 500) {
            badges.add(badge(BadgeType.RELIABLE, now))
        }
        return badges.distinctBy { it.id }
    }

    private fun badge(type: BadgeType, unlockedAt: Long, overrideName: String? = null): Badge =
        Badge(
            id = type.name.lowercase(),
            name = overrideName ?: type.title,
            icon = type.icon,
            description = type.description,
            unlockedAt = unlockedAt,
        )
}
