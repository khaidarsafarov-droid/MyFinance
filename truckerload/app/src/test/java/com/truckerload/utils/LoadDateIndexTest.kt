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
    fun build_marksExactJournalDate_notFullTripRange() {
        val load = sample(
            date = "2026-07-17",
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-08-05 18:00"),
            ),
        )
        val index = LoadDateIndex.build(listOf(load))

        assertEquals(setOf("2026-07-17"), index.keys)
        assertTrue("2026-07-17" in index)
        assertFalse("2026-07-18" in index)
        assertFalse("2026-08-01" in index)
        assertFalse("2026-08-05" in index)
        // Range logic still spans the trip — calendar dots must not.
        assertTrue("2026-08-05" in getLoadDateRange(load))
    }

    @Test
    fun build_skipsBlankDates() {
        val load = sample(date = "   ", stops = emptyList())
        assertTrue(LoadDateIndex.build(listOf(load)).isEmpty())
    }

    @Test
    fun markerDate_canonicalizesLooseInput() {
        val load = sample(date = "2026-07-17T12:00:00", stops = emptyList())
        assertEquals("2026-07-17", LoadDateIndex.markerDate(load))
    }

    private fun stop(type: StopType, scheduledTime: String) = Stop(
        id = type.name.hashCode() + scheduledTime.hashCode(),
        loadId = "id-1",
        stopNumber = 1,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = scheduledTime,
        timezone = "EDT",
        facilityCode = "",
        fullAddress = "X",
        city = "City",
        state = "ST",
        zip = "",
    )

    private fun sample(
        date: String,
        stops: List<Stop>,
    ) = Load(
        id = "id-1",
        tripId = "T-1",
        date = date,
        totalRate = 1000.0,
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
        stops = stops,
    )
}
