package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLoadGridTest {

    private fun load(id: String) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-08-01",
        totalRate = 100.0,
        totalMiles = 50.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 31,
        year = 2026,
        rawMessage = "",
        parsedAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun singleColumn_preservesOrderAsOneLoadPerRow() {
        val items = listOf(
            HomeListItem.LoadItem(load("a")),
            HomeListItem.LoadItem(load("b")),
        )
        val rows = buildHomeGridRows(items, columns = 1)
        assertEquals(2, rows.size)
        assertEquals(listOf("a"), (rows[0] as HomeGridRow.Loads).loads.map { it.id })
        assertEquals(listOf("b"), (rows[1] as HomeGridRow.Loads).loads.map { it.id })
    }

    @Test
    fun twoColumns_chunksConsecutiveLoads() {
        val items = listOf(
            HomeListItem.LoadItem(load("a")),
            HomeListItem.LoadItem(load("b")),
            HomeListItem.LoadItem(load("c")),
        )
        val rows = buildHomeGridRows(items, columns = 2)
        assertEquals(2, rows.size)
        assertEquals(listOf("a", "b"), (rows[0] as HomeGridRow.Loads).loads.map { it.id })
        assertEquals(listOf("c"), (rows[1] as HomeGridRow.Loads).loads.map { it.id })
    }

    @Test
    fun headersFlushPendingLoadsAndStayFullWidth() {
        val year = YearSection(2026, 1, 100.0, 50.0, emptyList())
        val month = MonthSection(2026, 8, "August", emptyList())
        val items = listOf(
            HomeListItem.YearHeader(year),
            HomeListItem.MonthHeader(month),
            HomeListItem.LoadItem(load("a")),
            HomeListItem.LoadItem(load("b")),
            HomeListItem.LoadItem(load("c")),
            HomeListItem.YearHeader(year.copy(year = 2025)),
            HomeListItem.LoadItem(load("d")),
        )
        val rows = buildHomeGridRows(items, columns = 2)
        assertTrue(rows[0] is HomeGridRow.FullWidth)
        assertTrue(rows[1] is HomeGridRow.FullWidth)
        assertEquals(listOf("a", "b"), (rows[2] as HomeGridRow.Loads).loads.map { it.id })
        assertEquals(listOf("c"), (rows[3] as HomeGridRow.Loads).loads.map { it.id })
        assertTrue(rows[4] is HomeGridRow.FullWidth)
        assertEquals(listOf("d"), (rows[5] as HomeGridRow.Loads).loads.map { it.id })
    }

    @Test
    fun filteredSectionHeader_isSkipped() {
        val totals = LoadFilterUseCase.Totals(1, 100.0, 50.0, 2.0)
        val items = listOf(
            HomeListItem.FilteredSectionHeader("Week", totals),
            HomeListItem.LoadItem(load("a")),
        )
        val rows = buildHomeGridRows(items, columns = 2)
        assertEquals(1, rows.size)
        assertEquals(listOf("a"), (rows[0] as HomeGridRow.Loads).loads.map { it.id })
    }

    @Test
    fun pagedLoadRowCount_roundsUp() {
        assertEquals(0, pagedLoadRowCount(0, 2))
        assertEquals(1, pagedLoadRowCount(1, 2))
        assertEquals(2, pagedLoadRowCount(3, 2))
        assertEquals(5, pagedLoadRowCount(10, 2))
    }
}
