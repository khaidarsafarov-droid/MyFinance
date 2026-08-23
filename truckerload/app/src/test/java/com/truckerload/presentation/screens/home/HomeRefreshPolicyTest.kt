package com.truckerload.presentation.screens.home

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRefreshPolicyTest {

    private val existing = listOf(sampleLoad("a"), sampleLoad("b"))

    @Test
    fun retainLoads_keepsPreviousWhenRefreshEmitsEmpty() {
        val retained = HomeRefreshPolicy.retainLoads(
            incoming = emptyList(),
            previous = existing,
            isRefreshing = true,
        )
        assertEquals(existing, retained)
    }

    @Test
    fun retainLoads_acceptsEmptyWhenNotRefreshing() {
        val retained = HomeRefreshPolicy.retainLoads(
            incoming = emptyList(),
            previous = existing,
            isRefreshing = false,
        )
        assertTrue(retained.isEmpty())
    }

    @Test
    fun retainLoads_acceptsIncomingWhenRefreshingHasData() {
        val next = listOf(sampleLoad("c"))
        val retained = HomeRefreshPolicy.retainLoads(
            incoming = next,
            previous = existing,
            isRefreshing = true,
        )
        assertEquals(next, retained)
    }

    @Test
    fun shouldShowEmptyJournal_hiddenWhilePagingRefreshLoads() {
        assertFalse(HomeRefreshPolicy.shouldShowEmptyJournal(0, pagingRefreshLoading = true))
        assertTrue(HomeRefreshPolicy.shouldShowEmptyJournal(0, pagingRefreshLoading = false))
        assertFalse(HomeRefreshPolicy.shouldShowEmptyJournal(3, pagingRefreshLoading = false))
    }

    @Test
    fun shouldShowInitialOverlay_hiddenDuringPullRefresh() {
        assertTrue(HomeRefreshPolicy.shouldShowInitialOverlay(isInitialLoading = true, isRefreshing = false))
        assertFalse(HomeRefreshPolicy.shouldShowInitialOverlay(isInitialLoading = true, isRefreshing = true))
        assertFalse(HomeRefreshPolicy.shouldShowInitialOverlay(isInitialLoading = false, isRefreshing = true))
    }

    private fun sampleLoad(id: String) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-08-23",
        totalRate = 1000.0,
        totalMiles = 400.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 34,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
