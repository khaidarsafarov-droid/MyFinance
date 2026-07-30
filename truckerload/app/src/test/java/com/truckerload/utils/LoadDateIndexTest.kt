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
    fun markerDates_usesExactLoadDate_notFullActiveRange() {
        val load = Load(
            id = "multi",
            tripId = "T-MULTI",
            date = "2026-07-28",
            totalRate = 2000.0,
            totalMiles = 500.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 31,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(1, "multi", 1, StopType.PU, "PU1", null, "2026-07-28 08:00", "EDT", null, "A", "A", "TX", ""),
                Stop(2, "multi", 2, StopType.DEL, null, null, "2026-08-02 18:00", "EDT", null, "B", "B", "TX", ""),
            ),
        )

        // Active range still spans into August for day filters…
        assertTrue("2026-08-01" in getLoadDateRange(load))
        assertTrue("2026-08-02" in getLoadDateRange(load))

        // …but calendar pears only sit on the exact journal date.
        val markers = LoadDateIndex.markerDates(listOf(load))
        assertEquals(setOf("2026-07-28"), markers)
        assertFalse("2026-08-01" in markers)
        assertFalse("2026-08-02" in markers)
    }

    @Test
    fun build_stillIndexesEveryActiveDay() {
        val load = Load(
            id = "multi",
            tripId = "T-MULTI",
            date = "2026-07-28",
            totalRate = 2000.0,
            totalMiles = 500.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 31,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(1, "multi", 1, StopType.PU, "PU1", null, "2026-07-28 08:00", "EDT", null, "A", "A", "TX", ""),
                Stop(2, "multi", 2, StopType.DEL, null, null, "2026-08-02 18:00", "EDT", null, "B", "B", "TX", ""),
            ),
        )
        val index = LoadDateIndex.build(listOf(load))
        assertTrue("2026-07-28" in index)
        assertTrue("2026-08-01" in index)
        assertTrue("2026-08-02" in index)
    }
}
