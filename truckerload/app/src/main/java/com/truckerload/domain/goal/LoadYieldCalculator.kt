package com.truckerload.domain.goal

import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import com.truckerload.utils.getPickUpDate
import java.time.LocalDate
import kotlin.math.ceil

private const val MS_PER_DAY = 86_400_000.0

object LoadYieldCalculator {

    /**
     * End of load: driver override (exact `"YYYY-MM-DD HH:mm"` or legacy date end-of-day)
     * or last DEL from Relay.
     */
    fun resolveFinishMillis(load: Load): Long? {
        ActualFinishDate.toMillis(load.actualFinishDate)?.let { return it }
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

    /**
     * Days the driver was actually on the road inside this reporting week.
     *
     * Each load covers pickup through its finish date, but not past [today] and not
     * outside the week. Overlapping loads count once. A delivery date that is still
     * in the future does not add days that have not happened, even when the driver
     * left that date unchanged.
     */
    fun onRoadDays(
        loads: List<Load>,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate,
    ): Int {
        if (loads.isEmpty() || today.isBefore(weekStart)) return 0
        val last = if (today.isBefore(weekEnd)) today else weekEnd
        val covered = HashSet<LocalDate>()
        for (load in loads) {
            val start = isoDate(getPickUpDate(load)) ?: continue
            val finish = isoDate(load.effectiveFinishDate()) ?: start
            if (finish.isBefore(weekStart) || start.isAfter(last)) continue
            var day = if (start.isAfter(weekStart)) start else weekStart
            val stop = if (finish.isBefore(last)) finish else last
            if (stop.isBefore(day)) continue
            while (!day.isAfter(stop)) {
                covered.add(day)
                day = day.plusDays(1)
            }
        }
        return covered.size
    }

    private fun isoDate(raw: String?): LocalDate? {
        val text = raw?.take(10)?.takeIf { it.length == 10 } ?: return null
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }
}
