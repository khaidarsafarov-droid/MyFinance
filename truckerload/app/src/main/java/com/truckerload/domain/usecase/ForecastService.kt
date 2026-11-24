package com.truckerload.domain.usecase

import com.truckerload.domain.money.sum
import com.truckerload.domain.store.LoadStore
import com.truckerload.domain.store.WeekSummaryStore
import com.truckerload.utils.shiftWeekNumberAndYear
import javax.inject.Inject

data class WeekForecast(
    val expectedRate: Double,
    val currentRate: Double,
    val progressPercent: Float,
    val trend: ForecastTrend,
    val deltaAmount: Double,
    val deltaPercent: Double,
    val basedOnWeeks: Int
)

enum class ForecastTrend { ABOVE, ON_TRACK, BELOW }

class ForecastService @Inject constructor(
    private val weeks: WeekSummaryStore,
    private val loads: LoadStore,
) {
    suspend fun calculateForecast(currentWeek: Int, currentYear: Int): WeekForecast? {
        // FIX: use shiftWeekNumberAndYear — hardcoding week 52 skips week 53 / wrong prior week
        val last8Weeks = (1..8).map { offset ->
            shiftWeekNumberAndYear(currentWeek, currentYear, -offset)
        }

        val summariesWithPaycheck = last8Weeks.map { (wn, wy) ->
            weeks.getWeekSummaryOnce(wn, wy)
        }.filter { it.paycheckAmount > 0 }

        if (summariesWithPaycheck.isEmpty()) return null

        val avgExpected = summariesWithPaycheck.map { it.totalLoadRate }.average()
        val currentLoads = loads.loadsForWeek(currentWeek, currentYear)
        val currentTotal = currentLoads.map { it.totalRate }.sum().cents / 100.0

        val progressPercent = if (avgExpected > 0) (currentTotal / avgExpected).toFloat().coerceIn(0f, 1.5f) else 0f
        val deltaAmount = currentTotal - avgExpected
        val deltaPercent = if (avgExpected > 0) ((currentTotal - avgExpected) / avgExpected) * 100 else 0.0

        val trend = when {
            currentTotal > avgExpected * 1.05 -> ForecastTrend.ABOVE
            currentTotal < avgExpected * 0.85 -> ForecastTrend.BELOW
            else -> ForecastTrend.ON_TRACK
        }

        return WeekForecast(
            expectedRate = avgExpected,
            currentRate = currentTotal,
            progressPercent = progressPercent,
            trend = trend,
            deltaAmount = deltaAmount,
            deltaPercent = deltaPercent,
            basedOnWeeks = summariesWithPaycheck.size
        )
    }
}
