package com.truckerload.domain.tax

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Test

class PerDiemCalculatorTest {

    @Test
    fun uniqueDays_countsEachCalendarDayOnce() {
        val load = loadWithStops(
            date = "2026-06-01",
            pu = "2026-06-01 08:00",
            del = "2026-06-03 18:00",
            year = 2026,
        )
        val dates = PerDiemCalculator.uniqueOnDutyDates(listOf(load), 2026)
        assertEquals(setOf("2026-06-01", "2026-06-02", "2026-06-03"), dates)
        assertEquals(3, PerDiemCalculator.dayCount(listOf(load), 2026))
        assertEquals(3 * 69.0, PerDiemCalculator.amount(listOf(load), 2026), 0.01)
    }

    @Test
    fun overlappingLoads_doNotDoubleCountSameDay() {
        val a = loadWithStops(
            date = "2026-06-01",
            pu = "2026-06-01 08:00",
            del = "2026-06-02 12:00",
            year = 2026,
            id = "a",
        )
        val b = loadWithStops(
            date = "2026-06-02",
            pu = "2026-06-02 14:00",
            del = "2026-06-02 20:00",
            year = 2026,
            id = "b",
        )
        assertEquals(2, PerDiemCalculator.dayCount(listOf(a, b), 2026))
        assertEquals(138.0, PerDiemCalculator.amount(listOf(a, b), 2026), 0.01)
    }

    @Test
    fun filtersToRequestedYearOnly() {
        val load = loadWithStops(
            date = "2025-12-31",
            pu = "2025-12-31 08:00",
            del = "2026-01-02 18:00",
            year = 2025,
        )
        assertEquals(setOf("2025-12-31"), PerDiemCalculator.uniqueOnDutyDates(listOf(load), 2025))
        assertEquals(
            setOf("2026-01-01", "2026-01-02"),
            PerDiemCalculator.uniqueOnDutyDates(listOf(load), 2026),
        )
    }

    private fun loadWithStops(
        date: String,
        pu: String,
        del: String,
        year: Int,
        id: String = date,
    ): Load = Load(
        id = id,
        tripId = id,
        date = date,
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = year,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = listOf(
            stop(id = 1, loadId = id, type = StopType.PU, scheduledTime = pu),
            stop(id = 2, loadId = id, type = StopType.DEL, scheduledTime = del),
        ),
    )

    private fun stop(
        id: Int,
        loadId: String,
        type: StopType,
        scheduledTime: String,
    ) = Stop(
        id = id,
        loadId = loadId,
        stopNumber = id,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = scheduledTime,
        timezone = "EDT",
        facilityCode = "",
        fullAddress = "City, ST",
        city = "City",
        state = "ST",
        zip = "",
    )
}
