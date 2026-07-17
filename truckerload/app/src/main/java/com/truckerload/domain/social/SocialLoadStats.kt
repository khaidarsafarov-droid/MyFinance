package com.truckerload.domain.social

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg

fun WeeklyLoadStatsAgg.leaderboardScore(category: LeaderboardCategory): Double =
    when (category) {
        LeaderboardCategory.LOADS -> loadCount.toDouble()
        LeaderboardCategory.REVENUE -> totalRevenue
        LeaderboardCategory.RPM ->
            if (totalMiles > 0) totalRevenue / totalMiles else 0.0
        LeaderboardCategory.OVERALL -> totalMiles
    }
