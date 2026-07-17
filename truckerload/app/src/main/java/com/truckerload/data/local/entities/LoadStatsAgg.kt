package com.truckerload.data.local.entities

/** All-time load totals for profile / badges (no stop hydration). */
data class LoadStatsAgg(
    val totalLoads: Int,
    val totalMiles: Double,
    val totalRevenue: Double,
)

/** Weekly load totals for leaderboard and challenges. */
data class WeeklyLoadStatsAgg(
    val loadCount: Int,
    val totalMiles: Double,
    val totalRevenue: Double,
)
