package com.truckerload.data.local.entities.analytics

data class WeeklyRevenueAgg(
    val weekNumber: Int,
    val year: Int,
    val gross: Double,
    val miles: Double,
    val loadCount: Int,
)
