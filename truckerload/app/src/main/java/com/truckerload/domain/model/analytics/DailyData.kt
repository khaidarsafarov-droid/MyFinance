package com.truckerload.domain.model.analytics

data class DailyData(
    val dayLabel: String,
    val dayOfWeek: Int,
    val gross: Double,
    val loadCount: Int,
)
