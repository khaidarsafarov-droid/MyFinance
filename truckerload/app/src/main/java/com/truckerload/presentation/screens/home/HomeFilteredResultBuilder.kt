package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money

/** Everything the Home journal needs to build one [FilteredResult]. */
internal data class HomeFilterInputs(
    val loads: List<Load>,
    val overlay: Map<String, Load>,
    val pendingDeletes: Set<String>,
    val filter: HomeFilterState,
    val allFilterTotals: LoadFilterUseCase.Totals?,
    val pagedTotals: LoadFilterUseCase.Totals?,
)

internal object HomeFilteredResultBuilder {

    val EMPTY = FilteredResult(emptyList(), LoadFilterUseCase.Totals(0, Money.ZERO, 0.0))

    /**
     * Merge Room with the optimistic overlay, apply the filter, and choose the totals source:
     * SQL totals for ALL, SQL totals for paged journals, else totals over the filtered list.
     * When Room paging draws the list, the in-memory list stays empty.
     */
    fun build(inputs: HomeFilterInputs, useCase: LoadFilterUseCase): FilteredResult {
        val filter = inputs.filter
        val merged = mergeLoadsWithOptimisticOverlay(inputs.loads, inputs.overlay, inputs.pendingDeletes)
        val filtered = useCase.filterLoads(
            loads = merged,
            filter = filter.filter,
            searchQuery = filter.searchQuery,
            selectedDate = filter.selectedDate,
            selectedWeekStart = filter.selectedWeekStart,
            selectedWeekEnd = filter.selectedWeekEnd,
            selectedYear = filter.selectedYear,
            selectedMonthYear = filter.selectedMonthYear,
            selectedMonth = filter.selectedMonth,
            dateIndex = null,
        )
        val totals = when {
            filter.filter == LoadFilter.ALL && inputs.allFilterTotals != null -> inputs.allFilterTotals
            inputs.pagedTotals != null -> inputs.pagedTotals
            else -> useCase.calculateTotals(filtered)
        }
        val hideInMemoryList =
            HomeRoomPagingPolicy.usesRoomPaging(filter.filter, filter.selectedYear) &&
                filter.searchQuery.isBlank()
        return FilteredResult(
            loads = if (hideInMemoryList) emptyList() else filtered,
            totals = totals,
        )
    }
}
