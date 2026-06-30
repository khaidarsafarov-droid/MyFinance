package com.truckerload.domain.model.analytics

data class WeekData(
    val weekNumber: Int,
    val year: Int,
    val label: String,
    val gross: Double,
    val miles: Double,
    val loadCount: Int,
)
