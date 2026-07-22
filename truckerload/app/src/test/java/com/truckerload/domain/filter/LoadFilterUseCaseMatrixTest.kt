package com.truckerload.domain.filter

import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getYesterdayDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadFilterUseCaseMatrixTest {

    private val useCase = LoadFilterUseCase()

    private fun load(
        id: String,
        date: String,
        tripId: String = id,
        dispute: Boolean = false,
        weekNumber: Int = 1,
        year: Int = 2026,
    ) = Load(
        id = id,
        tripId = tripId,
        date = date,
        totalRate = 100.0,
        totalMiles = 50.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = weekNumber,
        year = year,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        isDispute = dispute,
    )

    @Test
    fun disputeFilter() {
        val loads = listOf(load("a", "2026-07-01", dispute = true), load("b", "2026-07-02"))
        val filtered = useCase.filterLoads(loads, LoadFilter.DISPUTE, "", null, null, null, null)
        assertEquals(1, filtered.size)
        assertEquals("a", filtered[0].id)
    }

    @Test
    fun searchByTripId() {
        val loads = listOf(load("1", "2026-07-01", tripId = "T-AAA"), load("2", "2026-07-01", tripId = "T-BBB"))
        val filtered = useCase.filterLoads(loads, LoadFilter.ALL, "bbb", null, null, null, null)
        assertEquals(1, filtered.size)
        assertEquals("T-BBB", filtered[0].tripId)
    }

    @Test
    fun yearFilter() {
        val loads = listOf(load("1", "2025-12-31", year = 2025), load("2", "2026-01-01", year = 2026))
        val filtered = useCase.filterLoads(loads, LoadFilter.ALL, "", null, null, null, 2026)
        assertEquals(1, filtered.size)
        assertEquals("2", filtered[0].id)
    }

    @Test
    fun calendarDateFilter() {
        val loads = listOf(load("1", "2026-07-21"), load("2", "2026-07-22"))
        val filtered = useCase.filterLoads(loads, LoadFilter.CALENDAR_DATE, "", "2026-07-21", null, null, null)
        assertEquals(listOf("1"), filtered.map { it.id })
    }

    @Test
    fun calendarWeekFilterUsesWeekStart() {
        val (week, year) = getCurrentWeekNumberAndYear()
        val (start, end, _) = getWeekRange(week, year)
        val inWeek = load("in", start, weekNumber = week, year = year)
        val out = load("out", "2000-01-03", weekNumber = 1, year = 2000)
        val filtered = useCase.filterLoads(
            listOf(inWeek, out),
            LoadFilter.CALENDAR_WEEK,
            "",
            null,
            start,
            end,
            null,
        )
        assertEquals(listOf("in"), filtered.map { it.id })
    }

    @Test
    fun yesterdayFilterIncludesYesterdayDate() {
        val y = getYesterdayDate()
        val loads = listOf(load("y", y), load("other", "1999-01-01"))
        val filtered = useCase.filterLoads(loads, LoadFilter.YESTERDAY, "", null, null, null, null)
        assertTrue(filtered.any { it.id == "y" })
    }
}
