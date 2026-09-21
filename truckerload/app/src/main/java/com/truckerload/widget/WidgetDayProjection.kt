package com.truckerload.widget

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.utils.LoadDateIndex
import com.truckerload.utils.canonicalDateString
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Widget ring logic:
 *
 * - Days S…S are a selector (default = today; future days cannot be chosen).
 * - Ring / RPM on **today** show the **full reporting week** — the same loads as
 *   Home → This week. Tapping a past day shows Sunday…that day.
 * - Each load is chipped on its finish date when that date falls in the week
 *   (so PU last week / DEL this week still appears), otherwise PU / load.date.
 */
object WidgetDayProjection {

    data class DayTotals(
        val loadsCount: Int = 0,
        val gross: Double = 0.0,
        val miles: Double = 0.0,
    ) {
        val rpm: Double get() = if (miles > 0) gross / miles else 0.0
    }

    fun todayOffset(
        today: LocalDate = LocalDate.now(),
        weekStart: LocalDate = WidgetWeekDayHelper.startOfWeek(today),
    ): Int = ChronoUnit.DAYS.between(weekStart, today).toInt().coerceIn(0, 6)

    fun clampSelection(selectedOffset: Int?, todayOffset: Int): Int {
        val raw = selectedOffset ?: todayOffset
        return raw.coerceIn(0, todayOffset)
    }

    fun offsetForIso(isoDate: String, weekStart: LocalDate): Int? {
        val date = runCatching { LocalDate.parse(isoDate.take(10)) }.getOrNull() ?: return null
        val offset = ChronoUnit.DAYS.between(weekStart, date).toInt()
        return offset.takeIf { it in 0..6 }
    }

    /**
     * Date used to place [load] on a Sun–Sat chip in [weekStart]'s week.
     * Prefers the finish date (same field Home cards show) so a load that
     * reports in this week but picked up last week is not dropped.
     */
    fun isoDateForWeekChip(load: Load, weekStart: LocalDate): String? {
        val candidates = listOfNotNull(
            canonicalDateString(load.effectiveFinishDate()),
            LoadDateIndex.exactLoadDate(load),
        ).distinct()
        return candidates.firstOrNull { offsetForIso(it, weekStart) != null }
    }

    fun totalsByDay(loads: List<Load>, weekStart: LocalDate): List<DayTotals> {
        val days = Array(7) { DayTotals() }
        loads.forEach { load ->
            val iso = isoDateForWeekChip(load, weekStart) ?: return@forEach
            val offset = offsetForIso(iso, weekStart) ?: return@forEach
            val current = days[offset]
            days[offset] = current.copy(
                loadsCount = current.loadsCount + 1,
                gross = current.gross + load.totalRate,
                miles = current.miles + load.totalMiles,
            )
        }
        return days.toList()
    }

    fun maskFromDayTotals(days: List<DayTotals>): Int =
        days.foldIndexed(0) { index, mask, day ->
            if (day.loadsCount > 0) mask or (1 shl index) else mask
        }

    fun through(days: List<DayTotals>, endOffset: Int): DayTotals {
        if (days.isEmpty()) return DayTotals()
        val end = endOffset.coerceIn(0, days.lastIndex)
        val slice = days.subList(0, end + 1)
        return DayTotals(
            loadsCount = slice.sumOf { it.loadsCount },
            gross = slice.sumOf { it.gross },
            miles = slice.sumOf { it.miles },
        )
    }

    fun slicesOf(stats: WidgetStats): List<DayTotals> =
        (0..6).map { offset ->
            DayTotals(
                loadsCount = stats.dayLoads.getOrElse(offset) { 0 },
                gross = stats.dayGross.getOrElse(offset) { 0.0 },
                miles = stats.dayMiles.getOrElse(offset) { 0.0 },
            )
        }

    fun hasDaySlices(stats: WidgetStats): Boolean =
        stats.dayLoads.any { it > 0 } || stats.dayGross.any { it > 0.0 }

    fun isViewingToday(selectedOffset: Int?, todayOffset: Int): Boolean {
        val end = clampSelection(selectedOffset, todayOffset)
        return selectedOffset == null || end == todayOffset
    }

    fun project(
        week: WidgetStats,
        selectedOffset: Int?,
        todayOffset: Int = todayOffset(),
    ): WidgetStats {
        if (isViewingToday(selectedOffset, todayOffset)) return week
        val end = clampSelection(selectedOffset, todayOffset)
        if (!hasDaySlices(week)) {
            return week.copy(
                loadsCount = 0,
                totalLoadRate = 0.0,
                totalMiles = 0.0,
                avgCpm = 0.0,
                goalProgressPercent = 0f,
                goalRemainingAmount = week.weeklyProfitGoal,
            )
        }
        return applyToStats(week, through(slicesOf(week), end))
    }

    fun applyToStats(week: WidgetStats, projected: DayTotals): WidgetStats {
        val goal = week.weeklyProfitGoal
        val percent = if (goal > 0) {
            ((projected.gross / goal) * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
        return week.copy(
            loadsCount = projected.loadsCount,
            totalLoadRate = projected.gross,
            totalMiles = projected.miles,
            avgCpm = projected.rpm,
            goalProgressPercent = percent,
            goalRemainingAmount = (goal - projected.gross).coerceAtLeast(0.0),
        )
    }
}
