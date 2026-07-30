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
    fun markerDates_useJournalDateOnly_notFullTripSpan() {
        val load = sample(
            date = "2026-07-15",
            stops = listOf(
                stop(StopType.PU, "2026-07-15 08:00"),
                stop(StopType.DEL, "2026-07-18 18:00"),
            ),
        )
        // Full active range still spans mid-trip days for filters…
        assertTrue("2026-07-16" in getLoadDateRange(load))
        assertTrue("2026-07-17" in getLoadDateRange(load))
        // …but calendar pears mark only the journal date.
        val markers = LoadDateIndex.markerDates(listOf(load))
        assertEquals(setOf("2026-07-15"), markers)
        assertFalse("2026-07-16" in markers)
        assertFalse("2026-07-18" in markers)
    }

    @Test
    fun build_groupsMultipleLoadsOnSameDate() {
        val a = sample(id = "a", date = "2026-08-01")
        val b = sample(id = "b", date = "2026-08-01")
        val c = sample(id = "c", date = "2026-08-02")
        val index = LoadDateIndex.build(listOf(a, b, c))
        assertEquals(2, index.getValue("2026-08-01").size)
        assertEquals(listOf("c"), index.getValue("2026-08-02").map { it.id })
    }

    @Test
    fun build_skipsBlankDates() {
        val load = sample(date = "   ")
        assertTrue(LoadDateIndex.build(listOf(load)).isEmpty())
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
        id: String = "id-1",
        date: String = "2026-07-15",
        stops: List<Stop> = emptyList(),
    ) = Load(
        id = id,
        tripId = "T-$id",
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
