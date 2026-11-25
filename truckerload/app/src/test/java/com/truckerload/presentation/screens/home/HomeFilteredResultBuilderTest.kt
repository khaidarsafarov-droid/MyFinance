package com.truckerload.presentation.screens.home

import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.filter.LoadFilterUseCase
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFilteredResultBuilderTest {

    private val useCase = LoadFilterUseCase()

    private fun load(id: String, rate: Double, miles: Double = 500.0) = Load(
        id = id,
        tripId = "T-$id",
        date = "2026-11-18",
        totalRate = Money.fromDollars(rate),
        totalMiles = miles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 47,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )

    private fun inputs(
        loads: List<Load>,
        filter: HomeFilterState,
        overlay: Map<String, Load> = emptyMap(),
        pendingDeletes: Set<String> = emptySet(),
        allTotals: LoadFilterUseCase.Totals? = null,
        pagedTotals: LoadFilterUseCase.Totals? = null,
    ) = HomeFilterInputs(loads, overlay, pendingDeletes, filter, allTotals, pagedTotals)

    private val dayFilter = HomeFilterState(filter = LoadFilter.CALENDAR_DATE, selectedDate = "2026-11-18")

    @Test
    fun dayFilterListsLoadsAndSumsThemInMemory() {
        val result = HomeFilteredResultBuilder.build(
            inputs(listOf(load("a", 1000.0), load("b", 500.0)), dayFilter),
            useCase,
        )
        assertEquals(listOf("a", "b"), result.loads.map { it.id }.sorted())
        assertEquals(2, result.totals.loadCount)
        assertEquals(Money.fromDollars(1500.0), result.totals.totalRate)
    }

    @Test
    fun pendingDeleteHidesALoadAndOverlayAddsAnUnsavedOne() {
        val result = HomeFilteredResultBuilder.build(
            inputs(
                loads = listOf(load("a", 1000.0), load("b", 500.0)),
                filter = dayFilter,
                overlay = mapOf("c" to load("c", 250.0)),
                pendingDeletes = setOf("a"),
            ),
            useCase,
        )
        assertEquals(listOf("b", "c"), result.loads.map { it.id }.sorted())
        assertEquals(Money.fromDollars(750.0), result.totals.totalRate)
    }

    @Test
    fun allFilterPrefersSqlTotalsAndHidesTheInMemoryList() {
        val sql = LoadFilterUseCase.Totals(9, Money.fromDollars(9000.0), 4500.0)
        val result = HomeFilteredResultBuilder.build(
            inputs(
                loads = listOf(load("a", 1000.0)),
                filter = HomeFilterState(filter = LoadFilter.ALL, selectedYear = 2026),
                allTotals = sql,
            ),
            useCase,
        )
        assertEquals(sql, result.totals)
        assertTrue(result.loads.isEmpty())
    }

    @Test
    fun pagedTotalsWinOverInMemoryTotalsWhenThereIsNoAllTotals() {
        val paged = LoadFilterUseCase.Totals(3, Money.fromDollars(3000.0), 1500.0)
        val result = HomeFilteredResultBuilder.build(
            inputs(listOf(load("a", 1000.0)), dayFilter, pagedTotals = paged),
            useCase,
        )
        assertEquals(paged, result.totals)
        assertEquals(1, result.loads.size)
    }

    @Test
    fun emptyResultHasZeroTotals() {
        assertTrue(HomeFilteredResultBuilder.EMPTY.loads.isEmpty())
        assertEquals(0, HomeFilteredResultBuilder.EMPTY.totals.loadCount)
    }
}
