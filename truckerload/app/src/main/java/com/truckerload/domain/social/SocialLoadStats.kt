package com.truckerload.domain.social

import com.truckerload.domain.money.Money

/** Weekly totals the leaderboard scores. The data layer maps query rows into this. */
data class LeaderboardStats(
    val loadCount: Int,
    val totalMiles: Double,
    /** Cents. */
    val totalRevenueCents: Long,
)

fun LeaderboardStats.leaderboardScore(category: LeaderboardCategory): Double =
    when (category) {
        LeaderboardCategory.LOADS -> loadCount.toDouble()
        LeaderboardCategory.REVENUE -> Money.fromCents(totalRevenueCents).cents / 100.0
        LeaderboardCategory.RPM ->
            if (totalMiles > 0) Money.fromCents(totalRevenueCents).cents / 100.0 / totalMiles else 0.0
        LeaderboardCategory.OVERALL -> totalMiles
    }
