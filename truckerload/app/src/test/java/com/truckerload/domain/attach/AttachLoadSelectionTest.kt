package com.truckerload.domain.attach

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachLoadSelectionTest {

    private val week = 31
    private val year = 2026

    @Test
    fun quickPickThisWeek_takesNewestThreeOfCurrentWeek_ignoresOtherWeeksAndUpdatedAt() {
        val loads = listOf(
            // Other weeks / far future — must never appear in quick pick
            load("T-SEPT-A", "2026-09-04", weekNumber = 36, year = 2026, parsedAt = 9_000, updatedAt = 99_000),
            load("T-SEPT-B", "2026-09-05", weekNumber = 36, year = 2026, parsedAt = 9_100, updatedAt = 98_000),
            // This week (Sun 2026-07-26 … Sat 2026-08-01), week 31
            load("T-OLD", "2026-07-27", weekNumber = week, year = year, parsedAt = 100, updatedAt = 100),
            load("T-MID", "2026-07-28", weekNumber = week, year = year, parsedAt = 200, updatedAt = 200),
            load("T-NEW", "2026-07-30", weekNumber = week, year = year, parsedAt = 300, updatedAt = 50),
            load("T-EXTRA", "2026-07-29", weekNumber = week, year = year, parsedAt = 250, updatedAt = 250),
        )
        val pick = AttachLoadSelection.quickPickThisWeek(loads, week, year, limit = 3)
        assertEquals(listOf("T-NEW", "T-EXTRA", "T-MID"), pick.map { it.tripId })
    }

    @Test
    fun quickPickThisWeek_emptyWhenNoLoadsThisWeek() {
        val loads = listOf(
            load("T-SEPT", "2026-09-04", weekNumber = 36, year = 2026, parsedAt = 1, updatedAt = 9_999),
        )
        assertTrue(AttachLoadSelection.quickPickThisWeek(loads, week, year).isEmpty())
    }

    @Test
    fun filterBrowse_filtersByTripIdAndSortsByDate() {
        val loads = listOf(
            load("T-A", "2026-07-20", weekNumber = 30, year = 2026, parsedAt = 1, updatedAt = 1),
            load("T-B", "2026-07-30", weekNumber = week, year = year, parsedAt = 2, updatedAt = 2),
            load("OTHER", "2026-07-31", weekNumber = week, year = year, parsedAt = 3, updatedAt = 3),
        )
        val filtered = AttachLoadSelection.filterBrowse(loads, "T-")
        assertEquals(listOf("T-B", "T-A"), filtered.map { it.tripId })
    }

    private fun load(
        tripId: String,
        date: String,
        weekNumber: Int,
        year: Int,
        parsedAt: Long,
        updatedAt: Long,
    ) = Load(
        id = tripId,
        tripId = tripId,
        date = date,
        totalRate = 1000.0,
        totalMiles = 500.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = weekNumber,
        year = year,
        rawMessage = "",
        parsedAt = parsedAt,
        updatedAt = updatedAt,
    )
}
