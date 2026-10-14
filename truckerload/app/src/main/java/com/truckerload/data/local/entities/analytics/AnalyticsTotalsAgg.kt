package com.truckerload.data.local.entities.analytics

data class AnalyticsTotalsAgg(
    val loadCount: Int,
    /** Cents. */
    val gross: Long,
    val miles: Double,
)
