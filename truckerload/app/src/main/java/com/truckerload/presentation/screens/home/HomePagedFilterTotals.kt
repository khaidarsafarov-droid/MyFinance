package com.truckerload.presentation.screens.home

import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * SQL header totals for Room-paged Home journals (week / dispute).
 * Keeps numbers accurate without dual-hydrating the full load list.
 */
internal object HomePagedFilterTotals {
    fun WeeklyLoadStatsAgg.toTotals(): LoadFilterUseCase.Totals =
        LoadFilterUseCase.Totals(
            loadCount = loadCount,
            totalRate = totalRevenue,
            totalMiles = totalMiles,
            avgRpm = if (totalMiles > 0) totalRevenue / totalMiles else 0.0,
        )

    fun observe(
        state: HomeFilterState,
        loadRepository: LoadRepository,
    ): Flow<LoadFilterUseCase.Totals?> {
        return when (state.filter) {
            LoadFilter.THIS_WEEK -> {
                val (w, y) = getCurrentWeekNumberAndYear()
                loadRepository.watchWeeklyLoadStats(w, y).map { it.toTotals() }
            }
            LoadFilter.LAST_WEEK -> {
                val (w, y) = getPreviousWeekNumberAndYear()
                loadRepository.watchWeeklyLoadStats(w, y).map { it.toTotals() }
            }
            LoadFilter.CALENDAR_WEEK -> {
                val start = state.selectedWeekStart
                if (start.isNullOrBlank()) flowOf(null)
                else {
                    val (w, y) = getWeekNumberAndYearFromDate(start)
                    loadRepository.watchWeeklyLoadStats(w, y).map { it.toTotals() }
                }
            }
            LoadFilter.DISPUTE ->
                loadRepository.watchDisputeLoadStats().map { it.toTotals() }
            else -> flowOf(null)
        }
    }
}
