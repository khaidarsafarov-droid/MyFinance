package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeMonthSelectionTest {

    @Test
    fun withWholeMonth_setsThisMonthAndClearsWeekDate() {
        val state = HomeUiState(
            filter = LoadFilter.THIS_WEEK,
            selectedWeekStart = "2025-07-06",
            selectedWeekLabel = "week",
            selectedDate = "2025-07-07",
            selectedDateLabel = "day",
        ).withWholeMonth(2025, 8)
        assertEquals(LoadFilter.THIS_MONTH, state.filter)
        assertEquals(2025, state.selectedMonthYear)
        assertEquals(8, state.selectedMonth)
        assertEquals(formatHomeMonthLabel(8, 2025), state.selectedMonthLabel)
        assertNull(state.selectedWeekStart)
        assertEquals("", state.selectedWeekLabel)
        assertNull(state.selectedDate)
    }

    @Test
    fun clearedMonthSelection_dropsMonthFieldsOnly() {
        val state = HomeUiState(
            filter = LoadFilter.THIS_MONTH,
            selectedMonthYear = 2025,
            selectedMonth = 8,
            selectedMonthLabel = "August 2025",
            selectedWeekLabel = "keep-me",
        ).clearedMonthSelection()
        assertNull(state.selectedMonthYear)
        assertNull(state.selectedMonth)
        assertEquals("", state.selectedMonthLabel)
        assertEquals("keep-me", state.selectedWeekLabel)
        assertEquals(LoadFilter.THIS_MONTH, state.filter)
    }
}
