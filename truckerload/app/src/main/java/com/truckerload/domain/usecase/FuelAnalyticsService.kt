package com.truckerload.domain.usecase

import com.truckerload.domain.money.Money
import com.truckerload.domain.money.sum
import com.truckerload.domain.store.DieselStore
import com.truckerload.domain.store.LoadStore
import com.truckerload.utils.shiftWeekNumberAndYear
import javax.inject.Inject

data class FuelAnalytics(
    val periodLabel: String,
    val totalSpent: Double,
    val totalGallons: Double,
    val totalMiles: Double,
    val avgMpg: Double,
    val avgPricePerGallon: Double,
    val costPer100Miles: Double,
    val totalSavings: Double = 0.0,
    val previousPeriod: FuelAnalytics? = null
)

class FuelAnalyticsService @Inject constructor(
    private val diesel: DieselStore,
    private val loads: LoadStore,
) {
    suspend fun calculateForWeek(weekNumber: Int, year: Int): FuelAnalytics =
        calculateForWeek(weekNumber, year, includePrevious = true)

    /**
     * @param includePrevious when true, attaches [FuelAnalytics.previousPeriod] for the prior
     * week only (never nested — avoids infinite recursion).
     */
    private suspend fun calculateForWeek(
        weekNumber: Int,
        year: Int,
        includePrevious: Boolean,
    ): FuelAnalytics {
        val dieselRows = diesel.getDieselForWeekOnce(weekNumber, year)
        val loadRows = loads.loadsForWeek(weekNumber, year)
        val totalSpent = dieselRows.map { it.totalAmount }.sum().cents / 100.0
        val totalGallons = dieselRows.sumOf { it.gallons ?: 0.0 }
        val totalMiles = loadRows.sumOf { it.drivenMiles }
        val totalSavings = dieselRows.map { it.savingsAmount ?: Money.ZERO }.sum().cents / 100.0
        val avgMpg = if (totalGallons > 0) totalMiles / totalGallons else 0.0
        val avgPrice = if (totalGallons > 0) totalSpent / totalGallons else 0.0
        val costPer100 = if (totalMiles > 0) (totalSpent / totalMiles) * 100 else 0.0

        val prev = if (includePrevious) {
            // FIX: week-year aware previous week (not hardcoded 52)
            val (prevWeek, prevYear) = shiftWeekNumberAndYear(weekNumber, year, -1)
            calculateForWeek(prevWeek, prevYear, includePrevious = false)
        } else {
            null
        }

        return FuelAnalytics(
            periodLabel = "Week $weekNumber",
            totalSpent = totalSpent,
            totalGallons = totalGallons,
            totalMiles = totalMiles,
            avgMpg = avgMpg,
            avgPricePerGallon = avgPrice,
            costPer100Miles = costPer100,
            totalSavings = totalSavings,
            previousPeriod = prev
        )
    }
}
