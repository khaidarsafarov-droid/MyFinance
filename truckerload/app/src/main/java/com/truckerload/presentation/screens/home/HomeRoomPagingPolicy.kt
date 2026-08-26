package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter

/**
 * When Home should use Room [androidx.paging] for the journal list
 * instead of an in-memory filter over a hydrated snapshot.
 *
 * Day/month filters stay in-memory so they use active trip-day ranges
 * (stops), matching header totals and calendar day selection.
 */
internal object HomeRoomPagingPolicy {
    fun usesRoomPaging(filter: LoadFilter, selectedYear: Int?): Boolean {
        if (filter == LoadFilter.ALL) return true
        if (selectedYear != null) return false
        return when (filter) {
            LoadFilter.THIS_WEEK,
            LoadFilter.LAST_WEEK,
            LoadFilter.CALENDAR_WEEK,
            LoadFilter.DISPUTE,
            -> true
            else -> false
        }
    }
}
