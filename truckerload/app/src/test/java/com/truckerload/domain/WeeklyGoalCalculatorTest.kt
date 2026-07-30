package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyGoalCalculatorTest {

    @Test
    fun `daily pace uses week trip span from first PU to last finish`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = 2000.0,
                totalMiles = 800.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 24,
                year = 2026,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 2.0,
                stops = listOf(
                    com.truckerload.domain.model.Stop(
                        id = 1,
                        loadId = "1",
                        stopNumber = 1,
                        type = com.truckerload.domain.model.StopType.PU,
                        puNumber = null,
                        note = null,
                        scheduledTime = "2026-06-10 08:00",
                        timezone = "CDT",
                        facilityCode = "",
                        fullAddress = "Austin, TX",
                        city = "Austin",
                        state = "TX",
                        zip = "",
                    ),
                    com.truckerload.domain.model.Stop(
                        id = 2,
                        loadId = "1",
                        stopNumber = 2,
                        type = com.truckerload.domain.model.StopType.DEL,
                        puNumber = null,
                        note = null,
                        scheduledTime = "2026-06-11 18:00",
                        timezone = "CDT",
                        facilityCode = "",
                        fullAddress = "Dallas, TX",
                        city = "Dallas",
                        state = "TX",
                        zip = "",
                    ),
                ),
            ),
        )
        val pace = WeeklyGoalCalculator.calculateDailyPace(loads)
        assertEquals(1000.0, pace, 0.01)
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = 5000.0,
            weekLoads = loads,
            weekNumber = 24,
            year = 2026,
        )
        assertEquals(2.0, progress.totalActiveDays, 0.01)
    }

    @Test
    fun `progress marks behind when yield is low`() {
        val loads = listOf(
            Load(
                id = "1",
                tripId = "T1",
                date = "2026-06-10",
                totalRate = 500.0,
                totalMiles = 400.0,
                pointA = "Austin, TX",
                pointB = "Dallas, TX",
                puCount = 1,
                delCount = 1,
                weekNumber = 24,
                year = 2026,
                rawMessage = "",
                parsedAt = 1L,
                updatedAt = 1L,
                durationDays = 1.0,
            ),
        )
        val progress = WeeklyGoalCalculator.calculate(
            targetAmount = 5000.0,
            weekLoads = loads,
            weekNumber = 24,
            year = 2026,
        )
        assertEquals(PaceStatus.BEHIND, progress.paceStatus)
        assertEquals(500.0, progress.currentGross, 0.01)
    }
}
