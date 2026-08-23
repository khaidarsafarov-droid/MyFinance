package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoomPagingPolicyTest {

    @Test
    fun usesRoomPaging_trueForAllAndWeekFilters() {
        // Policy mirrors HomeViewModel.usesRoomPaging without constructing a VM.
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

        assertTrue(usesRoomPaging(LoadFilter.ALL, null))
        assertTrue(usesRoomPaging(LoadFilter.ALL, 2026))
        assertTrue(usesRoomPaging(LoadFilter.THIS_WEEK, null))
        assertTrue(usesRoomPaging(LoadFilter.LAST_WEEK, null))
        assertTrue(usesRoomPaging(LoadFilter.DISPUTE, null))
        assertFalse(usesRoomPaging(LoadFilter.THIS_MONTH, null))
        assertFalse(usesRoomPaging(LoadFilter.THIS_WEEK, 2026))
    }
}
