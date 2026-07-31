package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load

data class HomeUiState(
    val loads: List<Load> = emptyList(),
    val searchQuery: String = "",
    val filter: LoadFilter = LoadFilter.THIS_WEEK,
    val selectedYear: Int? = null,
    val selectedWeekStart: String? = null,
    val selectedWeekEnd: String? = null,
    val selectedWeekLabel: String = "",
    val selectedDate: String? = null,
    val selectedDateLabel: String = "",
    val isSearchExpanded: Boolean = false,
    val botStatusActive: Boolean = false,
    /** Surface non-fatal repository failures (delete/refresh). */
    val errorMessage: String? = null,
)

data class YearSection(
    val year: Int,
    val loadCount: Int,
    val totalRate: Double,
    val totalMiles: Double,
    val months: List<MonthSection>
)

data class MonthSection(
    val year: Int,
    val month: Int,
    val monthName: String,
    val loads: List<Load>
)

sealed class HomeListItem {
    data class YearHeader(val section: YearSection) : HomeListItem()
    data class MonthHeader(val section: MonthSection) : HomeListItem()
    data class FilteredSectionHeader(val label: String, val totals: LoadFilterUseCase.Totals) : HomeListItem()
    data class LoadItem(val load: Load) : HomeListItem()
}

/** Поля, влияющие на фильтрацию — отдельно от UI-флагов (поиск expanded и т.д.). */
internal data class HomeFilterState(
    val filter: LoadFilter = LoadFilter.THIS_WEEK,
    val searchQuery: String = "",
    val selectedDate: String? = null,
    val selectedWeekStart: String? = null,
    val selectedWeekEnd: String? = null,
    val selectedYear: Int? = null,
    val selectedDateLabel: String = "",
    val selectedWeekLabel: String = "",
)

/** Результат фильтрации: список, итоги (без полного journal для календаря). */
data class FilteredResult(
    val loads: List<Load>,
    val totals: LoadFilterUseCase.Totals,
    val datesWithLoads: Set<String> = emptySet(),
)
