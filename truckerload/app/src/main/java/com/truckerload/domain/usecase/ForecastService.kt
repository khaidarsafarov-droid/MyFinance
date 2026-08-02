package com.truckerload.domain.usecase

import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.WeekRepository
import com.truckerload.utils.shiftWeekNumberAndYear
import kotlinx.coroutines.flow.first

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

class ForecastService(
    private val weekRepository: WeekRepository,
    private val loadRepository: LoadRepository
) {
    suspend fun calculateForecast(currentWeek: Int, currentYear: Int): WeekForecast? {
        // FIX: use shiftWeekNumberAndYear — hardcoding week 52 skips week 53 / wrong prior week
        val last8Weeks = (1..8).map { offset ->
            shiftWeekNumberAndYear(currentWeek, currentYear, -offset)
        }

        val summariesWithPaycheck = last8Weeks.mapNotNull { (wn, wy) ->
            try {
                weekRepository.getWeekSummaryOnce(wn, wy)
            } catch (e: Exception) { android.util.Log.w("TL", "swallowed", e); null }
        }.filter { it.paycheckAmount > 0 }

        if (summariesWithPaycheck.isEmpty()) return null

        val avgExpected = summariesWithPaycheck.map { it.totalLoadRate }.average()
        val currentLoads = loadRepository.getLoadsByWeek(currentWeek, currentYear).first()
        val currentTotal = currentLoads.sumOf { it.totalRate }

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
