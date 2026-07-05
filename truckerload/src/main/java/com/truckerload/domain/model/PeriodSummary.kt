package com.truckerload.domain.model

/** Сводка по произвольному периоду (неделя, месяц, квартал). */
data class PeriodSummary(
    val periodLabel: String,
    val startDate: String,
    val endDate: String,
    val loadsCount: Int,
    val totalLoadRate: Double,
    val totalMiles: Double,
    val paycheckAmount: Double,
    val dieselAmount: Double,
    val netProfit: Double
)
