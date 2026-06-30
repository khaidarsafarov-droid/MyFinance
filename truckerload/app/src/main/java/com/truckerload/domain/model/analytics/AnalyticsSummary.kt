package com.truckerload.domain.model.analytics

data class AnalyticsSummary(
    val totalLoads: Int,
    val totalGross: Double,
    val totalMiles: Double,
    val avgRpm: Double,
    val avgGrossPerLoad: Double,
    val bestWeek: WeekData?,
)
