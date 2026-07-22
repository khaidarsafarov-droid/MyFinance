package com.truckerload.domain.filter

import com.truckerload.domain.model.Load
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.isLoadInWeek
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * QUALITY_100 #50 — Home THIS_WEEK totals match filtering Stats would use for current week.
 */
class StatsHomeWeekTotalsParityTest {

    private val useCase = LoadFilterUseCase()

    private fun load(
        id: String,
        date: String,
        rate: Double,
        miles: Double,
        weekNumber: Int,
        year: Int,
    ) = Load(
        id = id,
        tripId = id,
        date = date,
        totalRate = rate,
        totalMiles = miles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = weekNumber,
        year = year,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun thisWeekFilterTotals_matchIsLoadInWeekAggregation() {
        val (week, year) = getCurrentWeekNumberAndYear()
        val (start, _, _) = getWeekRange(week, year)
        val inWeek = load("in", start, 2500.0, 850.0, week, year)
        val out = load("out", "2000-01-03", 100.0, 10.0, 1, 2000)
        val all = listOf(inWeek, out)

        val homeFiltered = useCase.filterLoads(
            all,
            LoadFilter.THIS_WEEK,
            "",
            null,
            null,
            null,
            null,
        )
        val homeTotals = useCase.calculateTotals(homeFiltered)

        val statsStyle = all.filter { isLoadInWeek(it, week, year) }
        val statsTotals = useCase.calculateTotals(statsStyle)

        assertEquals(1, homeTotals.loadCount)
        assertEquals(homeTotals.loadCount, statsTotals.loadCount)
        assertEquals(homeTotals.totalRate, statsTotals.totalRate, 0.01)
        assertEquals(homeTotals.totalMiles, statsTotals.totalMiles, 0.01)
        assertEquals(2500.0, homeTotals.totalRate, 0.01)
    }
}
