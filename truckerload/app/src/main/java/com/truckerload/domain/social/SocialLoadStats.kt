package com.truckerload.domain.social

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.domain.money.Money

fun WeeklyLoadStatsAgg.leaderboardScore(category: LeaderboardCategory): Double =
    when (category) {
        LeaderboardCategory.LOADS -> loadCount.toDouble()
        LeaderboardCategory.REVENUE -> Money.fromCents(totalRevenue).cents / 100.0
        LeaderboardCategory.RPM ->
            if (totalMiles > 0) Money.fromCents(totalRevenue).cents / 100.0 / totalMiles else 0.0
        LeaderboardCategory.OVERALL -> totalMiles
    }
