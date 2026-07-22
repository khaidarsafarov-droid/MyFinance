package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.withRouteMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadYieldCalculatorTest {

    @Test
    fun `uses durationDays when set on load without stops`() {
        val load = sampleLoad(durationDays = 3.5)
        assertEquals(3.5, LoadYieldCalculator.loadActiveDurationDays(load), 0.001)
    }

    @Test
    fun `durationDays below one is coerced to one`() {
        val load = sampleLoad(durationDays = 0.2)
        assertEquals(1.0, LoadYieldCalculator.loadActiveDurationDays(load), 0.001)
    }

    @Test
    fun `actualDailyYield divides gross by summed active days`() {
        val loads = listOf(
            sampleLoad(durationDays = 2.0, totalRate = 1000.0),
            sampleLoad(durationDays = 2.0, totalRate = 600.0),
        )
        assertEquals(400.0, LoadYieldCalculator.actualDailyYield(loads), 0.001)
    }

    @Test
    fun `actualFinishDate shortens duration and raises pace`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val fromStops = sampleLoad(totalRate = 3019.11, stops = stops).withRouteMetrics()
        val early = fromStops.copy(actualFinishDate = "2026-07-18").withRouteMetrics()

        assertTrue(early.durationDays < fromStops.durationDays)
        assertTrue(early.pace > fromStops.pace)
        assertEquals(2.0, early.durationDays, 0.001)
    }

    @Test
    fun `clearing actualFinishDate restores stop-based duration`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val base = sampleLoad(totalRate = 1000.0, stops = stops).withRouteMetrics()
        val early = base.copy(actualFinishDate = "2026-07-18").withRouteMetrics()
        val cleared = early.copy(actualFinishDate = null).withRouteMetrics()
        assertEquals(base.durationDays, cleared.durationDays, 0.001)
    }

    private fun stop(
        type: StopType,
        scheduledTime: String,
        city: String,
        state: String,
    ) = Stop(
        id = "$type-$scheduledTime",
        loadId = "id-1",
        stopNumber = 1,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = scheduledTime,
        timezone = "EDT",
        facilityCode = "",
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )

    private fun sampleLoad(
        durationDays: Double = 0.0,
        totalRate: Double = 500.0,
        stops: List<Stop> = emptyList(),
    ): Load = Load(
        id = "id-1",
        tripId = "T-1",
        date = "2026-07-17",
        totalRate = totalRate,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 29,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        durationDays = durationDays,
        stops = stops,
    )
}
