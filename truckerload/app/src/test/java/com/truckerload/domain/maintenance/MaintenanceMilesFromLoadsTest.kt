package com.truckerload.domain.maintenance

import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceMilesFromLoadsTest {

    @Test
    fun excludesLoadsAlreadyInJournalBeforeReminder() {
        val createdAt = 1_000_000L
        val loads = listOf(
            // Already in journal before ТО — must NOT count (already in odometer).
            MaintenanceMilesFromLoads.LoadMiles("T-OLD", "1", "2026-07-24", 10_000.0, parsedAt = 500_000L),
            MaintenanceMilesFromLoads.LoadMiles("T-OLD2", "2", "2026-07-25", 4_030.0, parsedAt = 900_000L),
            // Added after reminder.
            MaintenanceMilesFromLoads.LoadMiles("T-NEW", "3", "2026-07-28", 1_502.0, parsedAt = 1_100_000L),
        )
        val result = MaintenanceMilesFromLoads.sumForTask(
            taskCreatedAt = createdAt,
            startDate = "2026-07-24",
            loads = loads,
        )
        assertEquals(1_502.0, result.miles, 0.01)
        assertEquals(1, result.loadCount)
    }

    @Test
    fun dedupesByTripId() {
        val createdAt = 1L
        val loads = listOf(
            MaintenanceMilesFromLoads.LoadMiles("T-1", "a", "2026-07-25", 800.0, parsedAt = 2L),
            MaintenanceMilesFromLoads.LoadMiles("T-1", "b", "2026-07-25", 800.0, parsedAt = 3L),
        )
        val result = MaintenanceMilesFromLoads.sumForTask(createdAt, "2026-07-24", loads)
        assertEquals(800.0, result.miles, 0.01)
        assertEquals(1, result.loadCount)
    }

    @Test
    fun ignoresLoadsBeforeStartDateEvenIfParsedLater() {
        val createdAt = 1L
        val loads = listOf(
            MaintenanceMilesFromLoads.LoadMiles("T-1", "a", "2026-07-20", 900.0, parsedAt = 2L),
            MaintenanceMilesFromLoads.LoadMiles("T-2", "b", "2026-07-25", 100.0, parsedAt = 2L),
        )
        val result = MaintenanceMilesFromLoads.sumForTask(createdAt, "2026-07-24", loads)
        assertEquals(100.0, result.miles, 0.01)
        assertEquals(1, result.loadCount)
    }
}
