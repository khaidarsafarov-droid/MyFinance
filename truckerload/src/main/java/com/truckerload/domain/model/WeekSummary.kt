package com.truckerload.domain.model

data class WeekSummary(
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val loadsCount: Int,
    val totalLoadRate: Double,
    val totalMiles: Double,
    val paycheckAmount: Double,
    val hasPaycheck: Boolean,
    val dieselAmount: Double,
    val hasDiesel: Boolean,
    val netProfit: Double
)
