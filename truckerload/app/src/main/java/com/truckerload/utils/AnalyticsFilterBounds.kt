package com.truckerload.utils

import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import java.time.LocalDate

/** Inclusive ISO-date window. Empty [minDate] / [maxDate] means unbounded. */
data class AnalyticsDateBounds(
    val minDate: String,
    val maxDate: String,
)

fun availableAnalyticsYears(today: LocalDate = LocalDate.now()): List<Int> {
    val current = today.year
    return (current downTo (current - AnalyticsFilter.YEAR_COUNT + 1)).toList()
}

fun availableAnalyticsMonths(year: Int, today: LocalDate = LocalDate.now()): List<Int> {
    val lastMonth = when {
        year > today.year -> 0
        year == today.year -> today.monthValue
        else -> 12
    }
    return (1..lastMonth).toList()
}

fun availableAnalyticsWeeks(
    year: Int,
    month: Int,
    today: LocalDate = LocalDate.now(),
): List<Pair<Int, Int>> =
    getWeeksInMonth(year, month).filter { (week, weekYear) ->
        val start = LocalDate.parse(getWeekRange(week, weekYear).first)
        !start.isAfter(today)
    }

fun AnalyticsFilter.dateBounds(today: LocalDate = LocalDate.now()): AnalyticsDateBounds {
    weekNumber?.let { week ->
        val wy = weekYear ?: return@let
        val (start, end, _) = getWeekRange(week, wy)
        return AnalyticsDateBounds(start, end)
    }
    month?.let { m ->
        val y = year ?: return@let
        return boundsForWeeks(
            weeks = availableAnalyticsWeeks(y, m, today),
            fallbackMonth = m to y,
        )
    }
    year?.let { y ->
        val weeks = availableAnalyticsMonths(y, today)
            .flatMap { availableAnalyticsWeeks(y, it, today) }
        return boundsForWeeks(weeks = weeks, fallbackYear = y)
    }
    return when (preset ?: AnalyticsPeriod.LAST_12_WEEKS) {
        AnalyticsPeriod.LAST_12_WEEKS -> {
            val (firstWeek, firstYear) = enumerateRecentWeekSlots(12).first()
            AnalyticsDateBounds(getWeekRange(firstWeek, firstYear).first, "")
        }
        AnalyticsPeriod.LAST_6_MONTHS ->
            AnalyticsDateBounds(today.minusMonths(6).toString(), "")
        AnalyticsPeriod.ALL_TIME -> AnalyticsDateBounds("", "")
    }
}

/**
 * Explicit Sun–Sat slots to plot. `null` means “whatever weeks the query returned”
 * (all-time).
 */
fun AnalyticsFilter.weekSlots(today: LocalDate = LocalDate.now()): List<Pair<Int, Int>>? {
    weekNumber?.let { week ->
        val wy = weekYear ?: return@let
        return listOf(week to wy)
    }
    month?.let { m ->
        val y = year ?: return@let
        return availableAnalyticsWeeks(y, m, today)
    }
    year?.let { y ->
        return availableAnalyticsMonths(y, today)
            .flatMap { availableAnalyticsWeeks(y, it, today) }
    }
    return when (preset ?: AnalyticsPeriod.LAST_12_WEEKS) {
        AnalyticsPeriod.LAST_12_WEEKS -> enumerateRecentWeekSlots(12)
        AnalyticsPeriod.LAST_6_MONTHS -> enumerateRecentWeekSlots(26)
        AnalyticsPeriod.ALL_TIME -> null
    }
}

private fun boundsForWeeks(
    weeks: List<Pair<Int, Int>>,
    fallbackYear: Int? = null,
    fallbackMonth: Pair<Int, Int>? = null,
): AnalyticsDateBounds {
    if (weeks.isEmpty()) {
        fallbackMonth?.let { (month, year) ->
            val (start, end) = getMonthRange(month, year)
            return AnalyticsDateBounds(start, end)
        }
        fallbackYear?.let { year ->
            return AnalyticsDateBounds("$year-01-01", "$year-12-31")
        }
        return AnalyticsDateBounds("", "")
    }
    val first = weeks.first()
    val last = weeks.last()
    return AnalyticsDateBounds(
        minDate = getWeekRange(first.first, first.second).first,
        maxDate = getWeekRange(last.first, last.second).second,
    )
}
