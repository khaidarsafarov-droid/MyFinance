package com.truckerload.domain.social

object SocialBadges {
    fun compute(totalLoads: Int, totalMiles: Int, totalRevenue: Double): List<Badge> {
        val now = System.currentTimeMillis()
        val badges = mutableListOf<Badge>()
        if (totalLoads >= 100) {
            badges.add(Badge("loads_100", "100 грузов", "🏆", "Перевезено 100+ грузов", now))
        }
        if (totalMiles >= 50_000) {
            badges.add(Badge("miles_50k", "Покоритель дорог", "🗺️", "50 000+ миль", now))
        }
        if (totalRevenue >= 100_000) {
            badges.add(Badge("revenue_100k", "Золотой RPM", "⛽", "$100k+ гросса", now))
        }
        if (totalLoads >= 500) {
            badges.add(Badge("legend", "Легенда", "🏅", "500+ грузов", now))
        }
        return badges
    }
}
