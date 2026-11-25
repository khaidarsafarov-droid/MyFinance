package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeSelectionTransitionsTest {

    private val calendarWeek = HomeUiState(
        filter = LoadFilter.CALENDAR_WEEK,
        selectedWeekStart = "2026-11-16",
        selectedWeekEnd = "2026-11-22",
        selectedWeekLabel = "Week 47",
        selectedYear = 2026,
        selectedMonthYear = 2026,
        selectedMonth = 11,
        selectedMonthLabel = "November 2026",
    )

    @Test
    fun withFilter_keepsTheWeekOnlyForCalendarWeek() {
        val same = calendarWeek.withFilter(LoadFilter.CALENDAR_WEEK)
        assertEquals("2026-11-16", same.selectedWeekStart)
        assertEquals("Week 47", same.selectedWeekLabel)

        val other = calendarWeek.withFilter(LoadFilter.THIS_WEEK)
        assertNull(other.selectedWeekStart)
        assertNull(other.selectedWeekEnd)
        assertEquals("", other.selectedWeekLabel)
        assertNull(other.selectedYear)
    }

    @Test
    fun withFilter_alwaysClearsTheMonthSelection() {
        val next = calendarWeek.withFilter(LoadFilter.CALENDAR_WEEK)
        assertNull(next.selectedMonthYear)
        assertNull(next.selectedMonth)
        assertEquals("", next.selectedMonthLabel)
    }

    @Test
    fun withFilter_keepsTheArchiveYearOnlyForAll() {
        val all = HomeUiState(selectedYear = 2025).withFilter(LoadFilter.ALL)
        assertEquals(2025, all.selectedYear)
        assertNull(HomeUiState(selectedYear = 2025).withFilter(LoadFilter.LAST_WEEK).selectedYear)
    }

    @Test
    fun withCalendarWeek_replacesDateAndMonth() {
        val state = HomeUiState(selectedDate = "2026-11-18", selectedDateLabel = "Nov 18", selectedMonth = 11)
            .withCalendarWeek("2026-11-09", "2026-11-15", "Week 46")
        assertEquals(LoadFilter.CALENDAR_WEEK, state.filter)
        assertEquals("2026-11-09", state.selectedWeekStart)
        assertEquals("2026-11-15", state.selectedWeekEnd)
        assertEquals("Week 46", state.selectedWeekLabel)
        assertNull(state.selectedDate)
        assertEquals("", state.selectedDateLabel)
        assertNull(state.selectedMonth)
    }

    @Test
    fun withCalendarDate_replacesWeek() {
        val state = calendarWeek.withCalendarDate("2026-11-18", "Nov 18")
        assertEquals(LoadFilter.CALENDAR_DATE, state.filter)
        assertEquals("2026-11-18", state.selectedDate)
        assertEquals("Nov 18", state.selectedDateLabel)
        assertNull(state.selectedWeekStart)
        assertNull(state.selectedWeekEnd)
        assertEquals("", state.selectedWeekLabel)
        assertNull(state.selectedMonthYear)
    }

    @Test
    fun withArchiveYear_switchesToAll() {
        val state = HomeUiState(filter = LoadFilter.THIS_WEEK).withArchiveYear(2024)
        assertEquals(LoadFilter.ALL, state.filter)
        assertEquals(2024, state.selectedYear)
        assertNull(state.withArchiveYear(null).selectedYear)
    }
}
