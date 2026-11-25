package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter

/** Pure state changes behind the Home filter chips, calendar and archive picker. */

internal fun HomeUiState.withFilter(filter: LoadFilter): HomeUiState = copy(
    filter = filter,
    selectedYear = if (filter != LoadFilter.ALL) null else selectedYear,
    selectedMonthYear = null,
    selectedMonth = null,
    selectedMonthLabel = "",
    selectedWeekStart = if (filter == LoadFilter.CALENDAR_WEEK) selectedWeekStart else null,
    selectedWeekEnd = if (filter == LoadFilter.CALENDAR_WEEK) selectedWeekEnd else null,
    selectedWeekLabel = if (filter == LoadFilter.CALENDAR_WEEK) selectedWeekLabel else "",
    selectedDate = if (filter == LoadFilter.CALENDAR_DATE) selectedDate else null,
    selectedDateLabel = if (filter == LoadFilter.CALENDAR_DATE) selectedDateLabel else "",
)

internal fun HomeUiState.withCalendarWeek(start: String, end: String, label: String) = clearedMonthSelection().copy(
    filter = LoadFilter.CALENDAR_WEEK,
    selectedWeekStart = start,
    selectedWeekEnd = end,
    selectedWeekLabel = label,
    selectedDate = null,
    selectedDateLabel = "",
)

internal fun HomeUiState.withCalendarDate(date: String, label: String): HomeUiState = clearedMonthSelection().copy(
    filter = LoadFilter.CALENDAR_DATE,
    selectedDate = date,
    selectedDateLabel = label,
    selectedWeekStart = null,
    selectedWeekEnd = null,
    selectedWeekLabel = "",
)

internal fun HomeUiState.withArchiveYear(year: Int?): HomeUiState = copy(selectedYear = year, filter = LoadFilter.ALL)
