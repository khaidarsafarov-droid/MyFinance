package com.truckerload.widget

import com.truckerload.domain.model.Load
import com.truckerload.utils.LoadDateIndex
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Widget ring logic from the cabin sketch:
 *
 * - Days S…S are a selector (default = today; future days cannot be chosen).
 * - Ring / RPM show **week-to-date through the selected day**: gross, load count,
 *   miles, and RPM for Sunday…selected, against the unchanged weekly goal.
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

    fun totalsByDay(loads: List<Load>, weekStart: LocalDate): List<DayTotals> {
        val days = Array(7) { DayTotals() }
        loads.forEach { load ->
            val iso = LoadDateIndex.exactLoadDate(load) ?: return@forEach
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

    fun project(
        week: WidgetStats,
        selectedOffset: Int?,
        todayOffset: Int = todayOffset(),
    ): WidgetStats {
        val end = clampSelection(selectedOffset, todayOffset)
        if (!hasDaySlices(week)) {
            return if (end == todayOffset) week else week.copy(
                loadsCount = 0,
                totalLoadRate = 0.0,
                totalMiles = 0.0,
                avgCpm = 0.0,
                goalProgressPercent = 0f,
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
