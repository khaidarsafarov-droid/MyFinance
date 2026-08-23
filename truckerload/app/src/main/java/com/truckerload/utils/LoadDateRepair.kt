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

    /** Epoch before this is treated as unset/sentinel (tests often use `1L`). */
    private const val MIN_SANE_REFERENCE_MS = 946_684_800_000L // 2000-01-01 UTC

    /**
     * Prefer [anchorYearHint] / sane [Load.parsedAt] (Telegram message time) over a stored
     * [Load.date] year. History mislabeled as "this year" (e.g. Aug 2025 Pu-time stored as
     * 2026-08-…) is corrected on session repair and re-import.
     *
     * Wall-clock "now" is only a last-resort reference — it must not override a stored year
     * when [Load.parsedAt] is missing.
     */
    fun repair(
        load: Load,
        anchorYearHint: Int? = null,
        referenceMillis: Long? = null,
    ): Load {
        val explicitRef = saneReferenceMillis(referenceMillis)
            ?: saneReferenceMillis(load.parsedAt)
        val refMillis = explicitRef ?: System.currentTimeMillis()
        val messageYear = explicitRef?.let { yearFromMillis(it) }
        val yearHint = load.date.take(4).toIntOrNull()?.takeIf { load.date.length >= 10 }
        // Message/parsedAt year wins over a wrong stored load.date year.
        val anchorYear = anchorYearHint
            ?: messageYear
            ?: yearHint
            ?: return load
        // Trust stored year only when it already matches the message/anchor year.
        val trustStoredYear = yearHint != null && yearHint == anchorYear
        val puDates = load.stops
            .filter { it.type == StopType.PU }
            .mapNotNull {
                parseDateFromScheduledTime(
                    s = it.scheduledTime,
                    defaultYear = anchorYear,
                    referenceMillis = refMillis,
                    trustDefaultYear = trustStoredYear,
                )
            }
        val delDates = load.stops
            .filter { it.type == StopType.DEL }
            .mapNotNull {
                parseDateFromScheduledTime(
                    s = it.scheduledTime,
                    defaultYear = anchorYear,
                    referenceMillis = refMillis,
                    trustDefaultYear = trustStoredYear,
                )
            }
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
     * Year for Relay `MM/DD` when only [referenceMillis] is known (message / parsedAt).
     * Picks the calendar year whose month/day is closest to the reference instant.
     */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        referenceMillis: Long,
    ): Int {
        val refYear = yearFromMillis(referenceMillis)
            ?: Calendar.getInstance().apply { timeInMillis = referenceMillis }.get(Calendar.YEAR)
        return resolveClosestYear(month, day, refYear, referenceMillis)
    }

    /**
     * Year for Relay `MM/DD` when an explicit [anchorYear] is known (message year or load.date).
     *
     * [referenceMillis] should be the Telegram message time / [Load.parsedAt] when available —
     * not wall-clock "now" on every hydrate/repair.
     *
     * Near-term bookings (~two weeks after the anchor) keep [anchorYear]; farther "future"
     * MM/DD from Telegram history is almost always the previous calendar year.
     */
    fun resolveRelayYear(
        month: Int,
        day: Int,
        anchorYear: Int,
        referenceMillis: Long,
    ): Int {
        if (month !in 1..12 || day !in 1..31) return anchorYear
        val candidateMs = calendarAt(anchorYear, month, day)
        val bookingHorizonMs = 14L * 24 * 60 * 60 * 1000
        if (candidateMs - referenceMillis > bookingHorizonMs) {
            return anchorYear - 1
        }
        return resolveClosestYear(month, day, anchorYear, referenceMillis)
    }

    private fun resolveClosestYear(
        month: Int,
        day: Int,
        centerYear: Int,
        referenceMillis: Long,
    ): Int {
        if (month !in 1..12 || day !in 1..31) return centerYear
        return (centerYear - 1..centerYear + 1).minBy { year ->
            abs(calendarAt(year, month, day) - referenceMillis)
        }
    }

    private fun calendarAt(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun yearFromMillis(millis: Long): Int? {
        val sane = saneReferenceMillis(millis) ?: return null
        return Calendar.getInstance().apply { timeInMillis = sane }.get(Calendar.YEAR)
    }

    private fun saneReferenceMillis(millis: Long?): Long? =
        millis?.takeIf { it >= MIN_SANE_REFERENCE_MS }
}
