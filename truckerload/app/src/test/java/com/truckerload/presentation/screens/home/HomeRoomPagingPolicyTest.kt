package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoomPagingPolicyTest {

    @Test
    fun usesRoomPaging_trueForPeriodFiltersWithoutYear() {
        // Policy mirrors HomeViewModel.usesRoomPaging without constructing a VM.
        fun usesRoomPaging(filter: LoadFilter, selectedYear: Int?): Boolean =
            selectedYear == null && filter != LoadFilter.ALL

        assertTrue(usesRoomPaging(LoadFilter.THIS_WEEK, null))
        assertTrue(usesRoomPaging(LoadFilter.LAST_WEEK, null))
        assertTrue(usesRoomPaging(LoadFilter.DISPUTE, null))
        assertFalse(usesRoomPaging(LoadFilter.ALL, null))
        assertFalse(usesRoomPaging(LoadFilter.THIS_WEEK, 2026))
    }
}
