package com.truckerload.domain.model

import com.truckerload.utils.getLoadDateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectiveFinishDateTest {

    @Test
    fun effectiveFinishDate_prefersManualOverride() {
        val load = sample(
            actualFinishDate = "2026-07-18",
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-07-20 18:00"),
            ),
        )
        assertEquals("2026-07-18", load.effectiveFinishDate())
    }

    @Test
    fun effectiveFinishDate_fallsBackToLastDel() {
        val load = sample(
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-07-19 12:00"),
                stop(StopType.DEL, "2026-07-20 18:00"),
            ),
        )
        assertEquals("2026-07-20", load.effectiveFinishDate())
    }

    @Test
    fun effectiveFinishDate_fallsBackToLoadDate() {
        val load = sample(date = "2026-07-15", stops = emptyList())
        assertEquals("2026-07-15", load.effectiveFinishDate())
    }

    @Test
    fun getLoadDateRange_respectsEarlyFinishOverride() {
        val load = sample(
            actualFinishDate = "2026-07-18",
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-07-20 18:00"),
            ),
        )
        val range = getLoadDateRange(load)
        assertTrue(range.contains("2026-07-17"))
        assertTrue(range.contains("2026-07-18"))
        assertFalse(range.contains("2026-07-19"))
        assertFalse(range.contains("2026-07-20"))
    }

    @Test
    fun effectiveFinishDate_blankOverrideFallsBackToDel() {
        val load = sample(
            actualFinishDate = "   ",
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-07-19 12:00"),
            ),
        )
        assertEquals("2026-07-19", load.effectiveFinishDate())
    }

    @Test
    fun withRouteMetrics_shortensDurationWhenFinishBeforeLastDel() {
        val load = sample(
            actualFinishDate = "2026-07-18",
            totalRate = 2000.0,
            stops = listOf(
                stop(StopType.PU, "2026-07-17 05:58"),
                stop(StopType.DEL, "2026-07-20 18:00"),
            ),
        ).withRouteMetrics()
        assertTrue(load.durationDays > 0.0)
        assertTrue(
            "duration should be shorter than full stop span",
            load.durationDays < 4.0,
        )
        assertEquals("2026-07-18", load.effectiveFinishDate())
    }

    private fun stop(type: StopType, scheduledTime: String) = Stop(
        id = "$type-$scheduledTime",
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
        date: String = "2026-07-17",
        actualFinishDate: String? = null,
        totalRate: Double = 1000.0,
        stops: List<Stop> = emptyList(),
    ) = Load(
        id = "id-1",
        tripId = "T-1",
        date = date,
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
        actualFinishDate = actualFinishDate,
        stops = stops,
    )
}
