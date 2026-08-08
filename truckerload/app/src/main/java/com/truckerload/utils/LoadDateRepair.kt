package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import java.util.Calendar
import kotlin.math.abs

/**
 * Corrects load.date / reporting week when Relay `MM/DD` times were anchored to the
 * wrong calendar year (e.g. Telegram history imported in 2026 with Pu-time `07/05`
 * becoming 2026-07-05 instead of the message year).
 */
object LoadDateRepair {

    /**
     * Prefer [anchorYearHint] (Telegram message year). Else use [Load.parsedAt] year
     * when the stored date's year disagrees with stop times re-parsed under that year.
     *
     * [referenceMillis] should be the Telegram message timestamp when available; otherwise
     * [Load.parsedAt] or device "now".
     */
    fun repair(
        load: Load,
        anchorYearHint: Int? = null,
        referenceMillis: Long? = null,
    ): Load {
        val ref = referenceMillis?.takeIf { it > 0L }
            ?: load.parsedAt.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val anchorYear = anchorYearHint
            ?: yearFromMillis(ref)
            ?: return load
        val puDates = load.stops
            .filter { it.type == StopType.PU }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear, ref) }
        val delDates = load.stops
            .filter { it.type == StopType.DEL }
            .mapNotNull { parseDateFromScheduledTime(it.scheduledTime, anchorYear, ref) }
        if (puDates.isEmpty() && delDates.isEmpty()) return load

        val repairedDate = puDates.minOrNull()
            ?: delDates.minOrNull()
            ?: load.date
        if (repairedDate == load.date && load.weekNumber > 0 && load.year > 0) {
            // Date already matches; still ensure reporting week is consistent.
            val (w, y) = getLoadReportingWeek(load)
            if (w == load.weekNumber && y == load.year) return load
        }
        return load.copy(date = repairedDate).withReportingWeek().withRouteMetrics()
    }

    /**
     * Year for Relay `MM/DD` without an explicit year: pick the calendar year whose
     * month/day is closest to [referenceMillis] (message date or import anchor).
     */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        referenceMillis: Long = System.currentTimeMillis(),
    ): Int {
        if (month !in 1..12 || day !in 1..31) {
            return Calendar.getInstance().apply { timeInMillis = referenceMillis }.get(Calendar.YEAR)
        }
        val refYear = Calendar.getInstance().apply { timeInMillis = referenceMillis }.get(Calendar.YEAR)
        fun atYear(year: Int): Long = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return (refYear - 1..refYear + 1).minBy { year -> abs(atYear(year) - referenceMillis) }
    }

    private fun yearFromMillis(millis: Long): Int? {
        if (millis <= 0L) return null
        return Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
    }
}
