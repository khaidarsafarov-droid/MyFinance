package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.utils.dateStringToEndOfDayMillis
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import kotlin.math.ceil

private const val MS_PER_DAY = 86_400_000.0

object LoadYieldCalculator {

    /** End of load: driver override (end of day) or last DEL from Relay. */
    fun resolveFinishMillis(load: Load): Long? {
        val override = load.actualFinishDate
            ?.takeIf { it.length >= 10 }
            ?.let { dateStringToEndOfDayMillis(it) }
        if (override != null) return override
        return getLastDeliveryMillis(load)
    }

    /**
     * Always compute active days from first PU → finish (override or last DEL).
     * Minimum 1 day.
     */
    fun computeActiveDurationDays(load: Load): Double {
        val startMs = getFirstPickUpMillis(load) ?: return 1.0
        val endMs = resolveFinishMillis(load) ?: return 1.0
        if (endMs <= startMs) return 1.0
        val days = ceil((endMs - startMs) / MS_PER_DAY).toInt()
        return days.coerceAtLeast(1).toDouble()
    }

    /**
     * Active days for one load.
     * Recomputes when stops or [Load.actualFinishDate] are available;
     * otherwise uses stored [Load.durationDays] (e.g. SQL / list without stops).
     */
    fun loadActiveDurationDays(load: Load): Double {
        if (load.stops.isNotEmpty() || !load.actualFinishDate.isNullOrBlank()) {
            return computeActiveDurationDays(load)
        }
        if (load.durationDays > 0.0) {
            return load.durationDays.coerceAtLeast(1.0)
        }
        return computeActiveDurationDays(load)
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
