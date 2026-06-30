package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import kotlin.math.ceil

private const val MS_PER_DAY = 86_400_000.0

object LoadYieldCalculator {

    /**
     * Active days for one load: ceil(PU → DEL in calendar days), minimum 1 day.
     * Falls back to load.date when PU/DEL millis are unavailable.
     */
    fun loadActiveDurationDays(load: Load): Double {
        if (load.durationDays > 0.0) {
            return load.durationDays.coerceAtLeast(1.0)
        }
        val startMs = getFirstPickUpMillis(load) ?: return 1.0
        val endMs = getLastDeliveryMillis(load) ?: return 1.0
        if (endMs <= startMs) return 1.0
        val days = ceil((endMs - startMs) / MS_PER_DAY).toInt()
        return days.coerceAtLeast(1).toDouble()
    }

    /**
     * Week pace: sum(gross) / sum(PU→DEL duration per load).
     */
    fun actualDailyYield(weekLoads: List<Load>): Double {
        if (weekLoads.isEmpty()) return 0.0
        val totalGross = weekLoads.sumOf { it.totalRate }
        if (totalGross <= 0.0) return 0.0
        val totalActiveDays = weekLoads.sumOf { loadActiveDurationDays(it) }
        if (totalActiveDays <= 0.0) return 0.0
        return GoalMoneyMath.roundMoney(totalGross / totalActiveDays)
    }

    fun totalActiveDays(weekLoads: List<Load>): Double =
        weekLoads.sumOf { loadActiveDurationDays(it) }
}
