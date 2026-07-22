package com.truckerload.domain.usecase

import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import kotlinx.coroutines.flow.first

data class FuelAnalytics(
    val periodLabel: String,
    val totalSpent: Double,
    val totalGallons: Double,
    val totalMiles: Double,
    val avgMpg: Double,
    val avgPricePerGallon: Double,
    val costPer100Miles: Double,
    val previousPeriod: FuelAnalytics? = null
)

class FuelAnalyticsService(
    private val dieselRepository: DieselRepository,
    private val loadRepository: LoadRepository
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
        val diesel = dieselRepository.getDieselForWeek(weekNumber, year).first()
        val loads = loadRepository.getLoadsByWeek(weekNumber, year).first()
        val totalSpent = diesel.sumOf { it.totalAmount }
        val totalGallons = diesel.sumOf { it.gallons ?: 0.0 }
        val totalMiles = loads.sumOf { it.totalMiles }
        val avgMpg = if (totalGallons > 0) totalMiles / totalGallons else 0.0
        val avgPrice = if (totalGallons > 0) totalSpent / totalGallons else 0.0
        val costPer100 = if (totalMiles > 0) (totalSpent / totalMiles) * 100 else 0.0

        val prev = if (includePrevious) {
            val prevWeek = if (weekNumber <= 1) 52 else weekNumber - 1
            val prevYear = if (weekNumber <= 1) year - 1 else year
            try {
                calculateForWeek(prevWeek, prevYear, includePrevious = false)
            } catch (e: Exception) {
                android.util.Log.w("TL", "fuel previous-week analytics failed", e)
                null
            }
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
            previousPeriod = prev
        )
    }
}
