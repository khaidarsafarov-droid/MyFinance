package com.truckerload.data.local.entities

/**
 * SQL aggregate for week efficiency: SUM(rate) / SUM(PU→DEL days per load).
 */
data class WeekYieldAgg(
    val totalGross: Double,
    val totalActiveDays: Double
)
