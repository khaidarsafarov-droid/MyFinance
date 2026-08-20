package com.truckerload.data.repository

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.getMonthRange
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Month/year stats must follow reporting weeks (Saturday owns the month) so a
 * Sun–Sat week that crosses a calendar boundary is not double-counted.
 */
class WeekRepositoryPeriodSummaryTest {

    private lateinit var loadRepository: LoadRepository
    private lateinit var paycheckRepository: PaycheckRepository
    private lateinit var dieselRepository: DieselRepository
    private lateinit var weekRepository: WeekRepository

    @Before
    fun setUp() {
        loadRepository = mock()
        paycheckRepository = mock()
        dieselRepository = mock()
        whenever(loadRepository.getLoadsByWeek(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(paycheckRepository.getPaychecksForWeek(any(), any())).thenReturn(flowOf(emptyList()))
        whenever(dieselRepository.getDieselForWeek(any(), any())).thenReturn(flowOf(emptyList()))
        weekRepository = WeekRepository(loadRepository, paycheckRepository, dieselRepository)
    }

    @Test
    fun spanningWeekPaycheckCountsInFebruaryOnly() = runBlocking {
        val (wn, wy) = getWeekNumberAndYearFromDate("2025-01-26")
        val (start, end, label) = getWeekRange(wn, wy)
        assertEquals("2025-01-26", start)
        assertEquals("2025-02-01", end)

        whenever(loadRepository.getLoadsByWeek(wn, wy)).thenReturn(
            flowOf(listOf(load(date = "2025-01-30", weekNumber = wn, year = wy, rate = 2500.0))),
        )
        whenever(paycheckRepository.getPaychecksForWeek(wn, wy)).thenReturn(
            flowOf(listOf(paycheck(weekNumber = wn, year = wy, start = start, end = end, net = 1800.0))),
        )
        whenever(dieselRepository.getDieselForWeek(wn, wy)).thenReturn(
            flowOf(listOf(diesel(weekNumber = wn, year = wy, start = start, end = end, amount = 400.0))),
        )

        val (janStart, janEnd) = getMonthRange(1, 2025)
        val (febStart, febEnd) = getMonthRange(2, 2025)
        val jan = weekRepository.getPeriodSummaryOnce(janStart, janEnd, "Jan")
        val feb = weekRepository.getPeriodSummaryOnce(febStart, febEnd, "Feb")

        assertEquals(0.0, jan.paycheckAmount, 0.01)
        assertEquals(0.0, jan.dieselAmount, 0.01)
        assertEquals(0, jan.loadsCount)
        assertEquals(1800.0, feb.paycheckAmount, 0.01)
        assertEquals(400.0, feb.dieselAmount, 0.01)
        assertEquals(2500.0, feb.totalLoadRate, 0.01)
        assertEquals(1, feb.loadsCount)
        assertEquals(1400.0, feb.netProfit, 0.01)
        assertEquals(1800.0, jan.paycheckAmount + feb.paycheckAmount, 0.01)

        val week = weekRepository.getWeekSummaryOnce(wn, wy)
        assertEquals(week.paycheckAmount, feb.paycheckAmount, 0.01)
        assertEquals(week.totalLoadRate, feb.totalLoadRate, 0.01)
        assertEquals(label, week.weekLabel)
    }

    @Test
    fun yearBoundaryWeekCountsInNextYearOnly() = runBlocking {
        val (wn, wy) = getWeekNumberAndYearFromDate("2025-12-28")
        val (start, end, _) = getWeekRange(wn, wy)
        whenever(paycheckRepository.getPaychecksForWeek(wn, wy)).thenReturn(
            flowOf(listOf(paycheck(weekNumber = wn, year = wy, start = start, end = end, net = 900.0))),
        )

        val y2025 = weekRepository.getPeriodSummaryOnce("2025-01-01", "2025-12-31", "2025")
        val y2026 = weekRepository.getPeriodSummaryOnce("2026-01-01", "2026-12-31", "2026")
        assertEquals(0.0, y2025.paycheckAmount, 0.01)
        assertEquals(900.0, y2026.paycheckAmount, 0.01)
        assertEquals(900.0, y2025.paycheckAmount + y2026.paycheckAmount, 0.01)
    }

    private fun load(date: String, weekNumber: Int, year: Int, rate: Double) = Load(
        id = "load-$date",
        tripId = "T-SPAN",
        date = date,
        totalRate = rate,
        totalMiles = 800.0,
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

    private fun paycheck(
        weekNumber: Int,
        year: Int,
        start: String,
        end: String,
        net: Double,
    ) = Paycheck(
        id = 1,
        weekNumber = weekNumber,
        year = year,
        weekLabel = "",
        weekStartDate = start,
        weekEndDate = end,
        driverName = null,
        grossAmount = net,
        netAmount = net,
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private fun diesel(
        weekNumber: Int,
        year: Int,
        start: String,
        end: String,
        amount: Double,
    ) = Diesel(
        id = 1,
        weekNumber = weekNumber,
        year = year,
        weekLabel = "",
        weekStartDate = start,
        weekEndDate = end,
        totalAmount = amount,
        gallons = null,
        pricePerGallon = null,
        location = null,
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )
}
