package com.truckerload.domain.goal

import com.truckerload.domain.money.Money
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
    fun actualDailyYield(weekLoads: List<Load>): Money {
        if (weekLoads.isEmpty()) return Money.ZERO
        val totalGross = weekLoads.fold(Money.ZERO) { acc, load -> acc + load.totalRate }
        if (totalGross.cents <= 0L) return Money.ZERO
        val totalActiveDays = weekLoads.sumOf { loadActiveDurationDays(it) }
        if (totalActiveDays <= 0.0) return Money.ZERO
        return totalGross.dividedBy(kotlin.math.ceil(totalActiveDays).toInt().coerceAtLeast(1))
    }

    fun totalActiveDays(weekLoads: List<Load>): Double =
        weekLoads.sumOf { loadActiveDurationDays(it) }

    /**
     * Days the driver was actually on the road inside this reporting week.
     *
     * Same windows as [coveredDates]: own pickup through own finish, cut to the
     * next pickup when that comes first, and never past [today].
     */
    fun onRoadDays(
        loads: List<Load>,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        today: LocalDate,
    ): Int {
        if (loads.isEmpty() || today.isBefore(weekStart)) return 0
        val last = if (today.isBefore(weekEnd)) today else weekEnd
        return coveredDates(loads, last).count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
    }

    /**
     * Calendar days a driver is on the road, through [through] inclusive.
     *
     * Default window is the load's pickup through its own finish. When the next
     * load is picked up sooner, the earlier load ends on that pickup day. The
     * stored delivery date is left unchanged. The same day is never returned twice.
     */
    fun coveredDates(loads: List<Load>, through: LocalDate): Set<LocalDate> {
        val ordered = loads.mapNotNull { load ->
            val start = isoDate(getPickUpDate(load)) ?: return@mapNotNull null
            load to start
        }.sortedBy { it.second }
        val covered = HashSet<LocalDate>()
        for (index in ordered.indices) {
            val (load, start) = ordered[index]
            if (start.isAfter(through)) continue
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
            val stop = if (finish.isBefore(through)) finish else through
            if (stop.isBefore(start)) continue
            var day = start
            while (!day.isAfter(stop)) {
                covered.add(day)
                day = day.plusDays(1)
            }
        }
        return covered
    }

    private fun isoDate(raw: String?): LocalDate? {
        val text = raw?.take(10)?.takeIf { it.length == 10 } ?: return null
        return runCatching { LocalDate.parse(text) }.getOrNull()
    }
}
