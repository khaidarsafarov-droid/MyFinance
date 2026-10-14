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
     * By default a load runs from its pickup to its own finish. When a later load
     * is picked up before that finish, the earlier load ends on the later pickup
     * day — the driver does not have to edit the first delivery date. Days after
     * [today] are not counted, and the same calendar day is never counted twice.
     */
    fun onRoadDays(
        loads: List<Load>,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate,
    ): Int {
        if (loads.isEmpty() || today.isBefore(weekStart)) return 0
        val last = if (today.isBefore(weekEnd)) today else weekEnd
        val ordered = loads.mapNotNull { load ->
            val start = isoDate(getPickUpDate(load)) ?: return@mapNotNull null
            load to start
        }.sortedBy { it.second }
        val covered = HashSet<LocalDate>()
        for (index in ordered.indices) {
            val (load, start) = ordered[index]
            val ownFinish = isoDate(load.effectiveFinishDate()) ?: start
            val nextPickup = ordered.getOrNull(index + 1)?.second
            val finish = if (
                nextPickup != null &&
                nextPickup.isAfter(start) &&
                nextPickup.isBefore(ownFinish)
            ) {
                nextPickup
            } else {
                ownFinish
            }
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
