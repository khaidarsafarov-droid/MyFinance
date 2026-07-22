package com.truckerload.domain.social

object SocialBadges {
    fun compute(totalLoads: Int, totalMiles: Int, totalRevenue: Double): List<Badge> {
        val now = System.currentTimeMillis()
        val badges = mutableListOf<Badge>()
        if (totalLoads >= 100) {
            badges.add(Badge("loads_100", "100 loads", "🏆", "100+ loads hauled", now))
        }
        if (totalMiles >= 50_000) {
            badges.add(Badge("miles_50k", "Road Warrior", "🗺️", "50,000+ miles", now))
        }
        if (totalRevenue >= 100_000) {
            badges.add(Badge("revenue_100k", "Golden RPM", "⛽", "$100k+ gross", now))
        }
        if (totalLoads >= 500) {
            badges.add(Badge("legend", "Legend", "🏅", "500+ loads", now))
        }
        return badges
    }
}
