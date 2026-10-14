package com.truckerload.data.local.entities.analytics

data class DailyGrossAgg(
    val dayOfWeek: Int,
    /** Cents. */
    val gross: Long,
    val loadCount: Int,
)
