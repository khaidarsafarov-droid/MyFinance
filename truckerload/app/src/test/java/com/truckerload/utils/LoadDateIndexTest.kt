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
    fun build_indexesEveryActiveDayInTripSpan() {
        val load = multiDayLoad(
            date = "2026-07-30",
            puTime = "2026-07-30 08:00",
            delTime = "2026-08-02 18:00",
        )
        val index = LoadDateIndex.build(listOf(load))
        assertTrue("2026-07-30" in index)
        assertTrue("2026-07-31" in index)
        assertTrue("2026-08-01" in index)
        assertTrue("2026-08-02" in index)
    }

    @Test
    fun markerDates_usesOnlyExactLoadDate_notFilledTripSpan() {
        val load = multiDayLoad(
            date = "2026-07-30",
            puTime = "2026-07-30 08:00",
            delTime = "2026-08-02 18:00",
        )
        val markers = LoadDateIndex.markerDates(listOf(load))
        assertEquals(setOf("2026-07-30"), markers)
        assertFalse("Future August days must not get calendar pears from a July load",
            markers.any { it.startsWith("2026-08") })
    }

    @Test
    fun markerDates_skipsBlankOrInvalidDates() {
        val good = multiDayLoad(date = "2026-07-28", puTime = "2026-07-28 08:00", delTime = "2026-07-29 18:00")
        val bad = good.copy(id = "bad", date = "not-a-date")
        assertEquals(setOf("2026-07-28"), LoadDateIndex.markerDates(listOf(good, bad)))
    }

    private fun multiDayLoad(date: String, puTime: String, delTime: String): Load = Load(
        id = "multi",
        tripId = "T-MULTI",
        date = date,
        totalRate = 2000.0,
        totalMiles = 500.0,
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
            Stop(
                id = 1,
                loadId = "multi",
                stopNumber = 1,
                type = StopType.PU,
                puNumber = "PU1",
                note = null,
                scheduledTime = puTime,
                timezone = "EDT",
                facilityCode = null,
                fullAddress = "A",
                city = "A",
                state = "NC",
                zip = "",
            ),
            Stop(
                id = 2,
                loadId = "multi",
                stopNumber = 2,
                type = StopType.DEL,
                puNumber = null,
                note = null,
                scheduledTime = delTime,
                timezone = "EDT",
                facilityCode = null,
                fullAddress = "B",
                city = "B",
                state = "TX",
                zip = "",
            ),
        ),
    )
}
