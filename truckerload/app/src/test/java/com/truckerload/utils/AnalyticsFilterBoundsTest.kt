package com.truckerload.utils

import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsFilterBoundsTest {

    private val today = LocalDate.of(2026, 8, 26)

    @Test
    fun availableYears_areCurrentDownFive() {
        assertEquals(listOf(2026, 2025, 2024, 2023, 2022), availableAnalyticsYears(today))
    }

    @Test
    fun availableMonths_stopAtCurrentMonth() {
        assertEquals((1..8).toList(), availableAnalyticsMonths(2026, today))
        assertEquals((1..12).toList(), availableAnalyticsMonths(2025, today))
        assertTrue(availableAnalyticsMonths(2027, today).isEmpty())
    }

    @Test
    fun availableWeeks_includeInProgressWeek_hideFutureWeeks() {
        val weeks = availableAnalyticsWeeks(2026, 8, today)
        val current = getWeekNumberAndYearFromDate("2026-08-26")
        assertTrue(weeks.contains(current))
        weeks.forEach { (week, year) ->
            val start = LocalDate.parse(getWeekRange(week, year).first)
            assertFalse(start.isAfter(today))
        }
    }

    @Test
    fun yearBounds_unionOfOwnedWeeksThroughToday() {
        val filter = AnalyticsFilter.preset(AnalyticsPeriod.ALL_TIME).selectYear(2025)
        val weeks = (1..12).flatMap { availableAnalyticsWeeks(2025, it, today) }
        val bounds = filter.dateBounds(today)
        assertEquals(getWeekRange(weeks.first().first, weeks.first().second).first, bounds.minDate)
        assertEquals(getWeekRange(weeks.last().first, weeks.last().second).second, bounds.maxDate)
    }

    @Test
    fun february2025_monthBounds_matchOwnedWeeks() {
        val filter = AnalyticsFilter.DEFAULT.selectYear(2025).selectMonth(2)
        val weeks = getWeeksInMonth(2025, 2)
        val bounds = filter.dateBounds(today)
        assertEquals(getWeekRange(weeks.first().first, weeks.first().second).first, bounds.minDate)
        assertEquals(getWeekRange(weeks.last().first, weeks.last().second).second, bounds.maxDate)
        assertEquals(weeks, filter.weekSlots(today))
    }

    @Test
    fun spanningWeek_belongsToSaturdayMonth() {
        val spanning = getWeekNumberAndYearFromDate("2025-01-26")
        val february = AnalyticsFilter.DEFAULT.selectYear(2025).selectMonth(2)
        assertTrue(february.weekSlots(today)!!.contains(spanning))

        val january = AnalyticsFilter.DEFAULT.selectYear(2025).selectMonth(1)
        assertFalse(january.weekSlots(today)!!.contains(spanning))

        val weekFilter = february.selectWeek(spanning.first, spanning.second)
        val (start, end, _) = getWeekRange(spanning.first, spanning.second)
        assertEquals(AnalyticsDateBounds(start, end), weekFilter.dateBounds(today))
    }

    @Test
    fun last12Weeks_isOpenEndedFromFirstSunday() {
        val bounds = AnalyticsFilter.DEFAULT.dateBounds(today)
        val (firstWeek, firstYear) = enumerateRecentWeekSlots(12).first()
        assertEquals(getWeekRange(firstWeek, firstYear).first, bounds.minDate)
        assertEquals("", bounds.maxDate)
    }

    @Test
    fun allTime_isUnbounded() {
        val bounds = AnalyticsFilter.preset(AnalyticsPeriod.ALL_TIME).dateBounds(today)
        assertEquals("", bounds.minDate)
        assertEquals("", bounds.maxDate)
        assertEquals(null, AnalyticsFilter.preset(AnalyticsPeriod.ALL_TIME).weekSlots(today))
    }
}
