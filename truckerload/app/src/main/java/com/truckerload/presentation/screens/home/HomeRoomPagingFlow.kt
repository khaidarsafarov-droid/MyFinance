package com.truckerload.presentation.screens.home

import androidx.paging.PagingData
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getPreviousWeekNumberAndYear
import com.truckerload.utils.getWeekNumberAndYearFromDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * True Room SQL paging for week / dispute / ALL journal filters.
 * Day and month filters stay in memory after a scoped overlap query so spanning trips match
 * header totals and calendar day selection.
 */
internal object HomeRoomPagingFlow {

    fun observe(filter: HomeFilterState, repository: LoadRepository): Flow<PagingData<Load>> {
        val query = filter.searchQuery.trim()
        if (query.isEmpty() && !HomeRoomPagingPolicy.usesRoomPaging(filter.filter, filter.selectedYear)) {
            return flowOf(PagingData.empty())
        }
        return when {
            query.isNotEmpty() -> repository.pagingLoads(searchQuery = query)

            filter.filter == LoadFilter.DISPUTE -> repository.pagingLoads(disputesOnly = true)

            filter.filter == LoadFilter.ALL -> repository.pagingLoads(journalYear = filter.selectedYear)

            filter.filter == LoadFilter.THIS_WEEK -> pagedWeek(repository, getCurrentWeekNumberAndYear())

            filter.filter == LoadFilter.LAST_WEEK -> pagedWeek(repository, getPreviousWeekNumberAndYear())

            filter.filter == LoadFilter.CALENDAR_WEEK && !filter.selectedWeekStart.isNullOrBlank() ->
                pagedWeek(repository, getWeekNumberAndYearFromDate(filter.selectedWeekStart))

            else -> repository.pagingLoads()
        }
    }

    private fun pagedWeek(repository: LoadRepository, week: Pair<Int, Int>): Flow<PagingData<Load>> =
        repository.pagingLoads(weekNumber = week.first, year = week.second)
}
