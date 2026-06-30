package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadYieldCalculatorTest {

    @Test
    fun `uses durationDays when set on load`() {
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

    private fun sampleLoad(
        durationDays: Double = 0.0,
        totalRate: Double = 500.0,
    ): Load = Load(
        id = "id-1",
        tripId = "T-1",
        date = "2026-06-01",
        totalRate = totalRate,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 23,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        durationDays = durationDays,
    )
}
