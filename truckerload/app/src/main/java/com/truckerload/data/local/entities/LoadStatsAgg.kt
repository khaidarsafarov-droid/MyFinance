package com.truckerload.data.local.entities

/** All-time load totals for profile / badges (no stop hydration). */
data class LoadStatsAgg(
    val totalLoads: Int,
    val totalMiles: Double,
    /** Cents. */
    val totalRevenue: Long,
)

/** Weekly load totals for leaderboard and challenges. */
data class WeeklyLoadStatsAgg(
    val loadCount: Int,
    val totalMiles: Double,
    /** Cents. */
    val totalRevenue: Long,
)

/** ТО miles counted since a service day. See [com.truckerload.data.local.dao.LoadDao.sumServiceMiles]. */
data class ServiceMilesAgg(
    val miles: Long,
    val loadCount: Int,
)
