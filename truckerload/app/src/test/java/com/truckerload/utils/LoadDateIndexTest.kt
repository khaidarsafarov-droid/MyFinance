package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadDateIndexTest {

    @Test
    fun build_marksExactLoadDate_notMidTripDays() {
        val load = Load(
            id = "cross-month",
            tripId = "T-CROSS",
            date = "2025-07-30",
            totalRate = 2000.0,
            totalMiles = 500.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 31,
            year = 2025,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(1, "cross-month", 1, StopType.PU, "PU1", null, "2025-07-30 08:00", "EDT", null, "A", "Garner", "NC", ""),
                Stop(2, "cross-month", 2, StopType.DEL, null, null, "2025-08-02 18:00", "EDT", null, "B", "Dallas", "TX", ""),
            ),
        )

        // Active range still spans into August for day filters…
        assertTrue("2025-08-01" in getLoadDateRange(load))
        assertTrue("2025-08-02" in getLoadDateRange(load))

        // …but calendar dots use only the exact journal date (PU / load.date).
        val dates = LoadDateIndex.build(listOf(load)).keys
        assertEquals(setOf("2025-07-30"), dates)
        assertFalse("2025-08-01" in dates)
        assertFalse("2025-08-02" in dates)
    }

    @Test
    fun exactLoadDate_usesCanonicalLoadDate() {
        val load = Load(
            id = "d1",
            tripId = "T-D1",
            date = "2025-07-15",
            totalRate = 1000.0,
            totalMiles = 100.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 0,
            weekNumber = 1,
            year = 2025,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = emptyList(),
        )
        assertEquals("2025-07-15", LoadDateIndex.exactLoadDate(load))
    }
}
