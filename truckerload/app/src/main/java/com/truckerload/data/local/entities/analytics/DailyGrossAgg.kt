package com.truckerload.data.local.entities.analytics

data class DailyGrossAgg(
    val dayOfWeek: Int,
    val gross: Double,
    val loadCount: Int,
)
