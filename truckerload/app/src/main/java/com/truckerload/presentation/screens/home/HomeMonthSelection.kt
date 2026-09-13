package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.utils.getWeekRange

internal fun HomeUiState.withWholeMonth(year: Int, month: Int): HomeUiState {
    val label = formatHomeMonthLabel(month, year)
    return copy(
        filter = LoadFilter.THIS_MONTH,
        selectedMonthYear = year,
        selectedMonth = month,
        selectedMonthLabel = label,
        selectedYear = null,
        selectedWeekStart = null,
        selectedWeekEnd = null,
        selectedWeekLabel = "",
        selectedDate = null,
        selectedDateLabel = "",
    )
}

internal fun HomeUiState.withMonthWeek(
    year: Int,
    month: Int,
    weekNumber: Int,
    weekYear: Int,
): HomeUiState {
    val monthLabel = formatHomeMonthLabel(month, year)
    val (start, end, weekLabel) = getWeekRange(weekNumber, weekYear)
    return copy(
        filter = LoadFilter.CALENDAR_WEEK,
        selectedMonthYear = year,
        selectedMonth = month,
        selectedMonthLabel = monthLabel,
        selectedWeekStart = start,
        selectedWeekEnd = end,
        selectedWeekLabel = weekLabel,
        selectedYear = null,
        selectedDate = null,
        selectedDateLabel = "",
    )
}

internal fun HomeUiState.clearedMonthSelection(): HomeUiState = copy(
    selectedMonthYear = null,
    selectedMonth = null,
    selectedMonthLabel = "",
)
