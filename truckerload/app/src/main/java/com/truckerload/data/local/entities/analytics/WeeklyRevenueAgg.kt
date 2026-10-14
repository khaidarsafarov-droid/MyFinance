package com.truckerload.data.local.entities.analytics

data class WeeklyRevenueAgg(
    val weekNumber: Int,
    val year: Int,
    /** Cents. */
    val gross: Long,
    val miles: Double,
    val loadCount: Int,
)
