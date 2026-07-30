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
    fun build_indexesEveryActiveTripDay() {
        val load = sample(
            date = "2026-07-26",
            pu = "07/26 08:00 EDT",
            del = "08/05 18:00 EDT",
        )
        val index = LoadDateIndex.build(listOf(load))
        assertTrue("2026-07-26" in index)
        assertTrue("2026-08-01" in index)
        assertTrue("2026-08-05" in index)
    }

    @Test
    fun calendarMarkerDates_usesExactLoadDateOnly_notFullTripRange() {
        val load = sample(
            date = "2026-07-26",
            pu = "07/26 08:00 EDT",
            del = "08/05 18:00 EDT",
        )
        val markers = LoadDateIndex.calendarMarkerDates(listOf(load), today = "2026-07-30")
        assertEquals(setOf("2026-07-26"), markers)
        assertFalse("2026-08-01" in markers)
        assertFalse("2026-08-05" in markers)
    }

    @Test
    fun calendarMarkerDates_hidesFutureLoadDates() {
        val july = sample(date = "2026-07-20", pu = "07/20 08:00 EDT", del = "07/21 08:00 EDT")
        val august = sample(date = "2026-08-15", pu = "08/15 08:00 EDT", del = "08/16 08:00 EDT")
        val markers = LoadDateIndex.calendarMarkerDates(
            listOf(july, august),
            today = "2026-07-30",
        )
        assertEquals(setOf("2026-07-20"), markers)
        assertFalse("2026-08-15" in markers)
    }

    @Test
    fun calendarMarkerDates_includesToday() {
        val todayLoad = sample(date = "2026-07-30", pu = "07/30 08:00 EDT", del = "07/30 18:00 EDT")
        val markers = LoadDateIndex.calendarMarkerDates(listOf(todayLoad), today = "2026-07-30")
        assertEquals(setOf("2026-07-30"), markers)
    }

    private fun sample(date: String, pu: String, del: String) = Load(
        id = "id-$date",
        tripId = "T-$date",
        date = date,
        totalRate = 1000.0,
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        stops = listOf(
            Stop(1, "id-$date", 1, StopType.PU, "PU1", null, pu, "EDT", null, "A", "A", "TX", ""),
            Stop(2, "id-$date", 2, StopType.DEL, null, null, del, "EDT", null, "B", "B", "TX", ""),
        ),
    )
}
