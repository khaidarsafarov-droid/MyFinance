package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRoomPagingPolicyTest {

    @Test
    fun usesRoomPaging_trueForAllAndWeekFilters() {
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.ALL, null))
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.ALL, 2026))
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.THIS_WEEK, null))
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.LAST_WEEK, null))
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.CALENDAR_WEEK, null))
        assertTrue(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.DISPUTE, null))
        assertFalse(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.THIS_MONTH, null))
        assertFalse(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.CALENDAR_DATE, null))
        assertFalse(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.YESTERDAY, null))
        assertFalse(HomeRoomPagingPolicy.usesRoomPaging(LoadFilter.THIS_WEEK, 2026))
    }
}
