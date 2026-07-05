package com.truckerload.domain.model

data class RouteStats(
    val pointA: String,
    val pointB: String,
    val routeKey: String,
    val totalLoads: Int,
    val totalEarned: Double,
    val totalMiles: Double,
    val avgRate: Double,
    val avgMiles: Double,
    val ratePerMile: Double,
    val bestLoad: Double,
    val worstLoad: Double,
    val lastUsed: String,
)

enum class RouteSortBy { RATE_PER_MILE, TOTAL_EARNED, LOAD_COUNT }
