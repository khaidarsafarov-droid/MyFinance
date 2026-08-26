package com.truckerload.presentation.screens.home

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.model.Load
import com.truckerload.utils.getYesterdayDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

/**
 * Room subscription for Home filters that still hydrate in memory
 * (day / month). Week / ALL / dispute lists use paging instead.
 *
 * Day and month queries overlap the denormalized PU–DEL span so a trip
 * that started yesterday still appears on today's calendar day.
 */
internal object HomeScopedLoadQuery {
    fun observe(
        filter: LoadFilter,
        selectedDate: String?,
        loadRepository: LoadRepository,
    ): Flow<List<Load>> = when (filter) {
        LoadFilter.THIS_WEEK,
        LoadFilter.LAST_WEEK,
        LoadFilter.CALENDAR_WEEK,
        LoadFilter.DISPUTE,
        LoadFilter.ALL,
        -> flowOf(emptyList())
        LoadFilter.THIS_MONTH -> {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val start = "%04d-%02d-01".format(year, month)
            val end = "%04d-%02d-%02d".format(
                year,
                month,
                cal.getActualMaximum(Calendar.DAY_OF_MONTH),
            )
            loadRepository.getLoadsOverlappingRange(start, end)
        }
        LoadFilter.YESTERDAY ->
            loadRepository.getLoadsOverlappingDay(getYesterdayDate())
        LoadFilter.CALENDAR_DATE -> {
            val date = selectedDate
            if (date.isNullOrBlank()) flowOf(emptyList())
            else loadRepository.getLoadsOverlappingDay(date)
        }
    }
}
