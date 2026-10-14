package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.model.lastDelDateFromStops
import com.truckerload.domain.model.withRouteMetrics
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadYieldCalculatorTest {

    @Test
    fun `uses durationDays when set on load without stops`() {
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

    @Test
    fun `actualFinishDate shortens duration and raises pace`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val fromStops = sampleLoad(totalRate = 3019.11, stops = stops).withRouteMetrics()
        val early = fromStops.copy(actualFinishDate = "2026-07-18").withRouteMetrics()

        assertTrue(early.durationDays < fromStops.durationDays)
        assertTrue(early.pace > fromStops.pace)
        assertEquals(2.0, early.durationDays, 0.001)
        assertEquals(1509.56, early.pace, 0.01)
    }

    @Test
    fun `resolveFinishMillis prefers actualFinishDate over last DEL`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val load = sampleLoad(stops = stops).copy(actualFinishDate = "2026-07-18")
        val finishMs = LoadYieldCalculator.resolveFinishMillis(load)
        val delMs = com.truckerload.utils.getLastDeliveryMillis(load)
        assertTrue(finishMs != null)
        assertTrue(delMs != null)
        assertTrue(finishMs!! < delMs!!)
    }

    @Test
    fun `resolveFinishMillis uses exact clock when time is set`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-21 18:00", "Aurora", "CO"),
        )
        val load = sampleLoad(stops = stops).copy(actualFinishDate = "2026-07-20 14:30")
        val finishMs = LoadYieldCalculator.resolveFinishMillis(load)
        val expected = com.truckerload.utils.parseScheduledTimeToMillis("2026-07-20 14:30")
        assertEquals(expected, finishMs)
    }

    @Test
    fun `clearing actualFinishDate restores stop-based duration`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val base = sampleLoad(totalRate = 1000.0, stops = stops).withRouteMetrics()
        val early = base.copy(actualFinishDate = "2026-07-18").withRouteMetrics()
        val cleared = early.copy(actualFinishDate = null).withRouteMetrics()
        assertEquals(base.durationDays, cleared.durationDays, 0.001)
    }

    @Test
    fun `lastDelDateFromStops picks latest DEL date`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-19 12:00", "Denver", "CO"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val load = sampleLoad(stops = stops)
        assertEquals("2026-07-20", load.lastDelDateFromStops())
        assertEquals("2026-07-20", load.effectiveFinishDate())
        assertEquals(
            "2026-07-18",
            load.copy(actualFinishDate = "2026-07-18").effectiveFinishDate(),
        )
    }

    @Test
    fun `onRoadDays stops at today even when delivery is still later`() {
        val weekStart = LocalDate.of(2026, 10, 11)
        val weekEnd = LocalDate.of(2026, 10, 17)
        val tuesday = LocalDate.of(2026, 10, 13)
        val plannedThroughFriday = sampleLoad(
            stops = listOf(
                stop(StopType.PU, "2026-10-11 08:00", "Garner", "NC"),
                stop(StopType.DEL, "2026-10-16 18:00", "Wilmington", "NC"),
            ),
        )
        val sameTripAgain = plannedThroughFriday.copy(id = "id-2", tripId = "T-2")
        assertEquals(
            3,
            LoadYieldCalculator.onRoadDays(listOf(plannedThroughFriday), weekStart, weekEnd, tuesday),
        )
        assertEquals(
            3,
            LoadYieldCalculator.onRoadDays(
                listOf(plannedThroughFriday, sameTripAgain),
                weekStart,
                weekEnd,
                tuesday,
            ),
        )
    }

    @Test
    fun `next pickup cuts the previous load on that pickup day`() {
        val weekStart = LocalDate.of(2026, 10, 11)
        val weekEnd = LocalDate.of(2026, 10, 17)
        val saturday = LocalDate.of(2026, 10, 17)
        val first = sampleLoad(
            stops = listOf(
                stop(StopType.PU, "2026-10-11 08:00", "Garner", "NC"),
                stop(StopType.DEL, "2026-10-16 18:00", "Wilmington", "NC"),
            ),
        )
        val second = sampleLoad(
            stops = listOf(
                stop(StopType.PU, "2026-10-14 09:00", "Wilmington", "NC"),
                stop(StopType.DEL, "2026-10-14 20:00", "Baltimore", "MD"),
            ),
        ).copy(id = "id-2", tripId = "T-2")
        assertEquals(
            4,
            LoadYieldCalculator.onRoadDays(listOf(first, second), weekStart, weekEnd, saturday),
        )
    }

    @Test
    fun `onRoadDays honors an earlier finish without counting days after it`() {
        val weekStart = LocalDate.of(2026, 10, 11)
        val weekEnd = LocalDate.of(2026, 10, 17)
        val tuesday = LocalDate.of(2026, 10, 13)
        val closedMonday = sampleLoad(
            stops = listOf(
                stop(StopType.PU, "2026-10-11 08:00", "Garner", "NC"),
                stop(StopType.DEL, "2026-10-16 18:00", "Wilmington", "NC"),
            ),
        ).copy(actualFinishDate = "2026-10-12")
        assertEquals(
            2,
            LoadYieldCalculator.onRoadDays(listOf(closedMonday), weekStart, weekEnd, tuesday),
        )
    }

    @Test
    fun `finish override earlier than last DEL shortens active days`() {
        val stops = listOf(
            stop(StopType.PU, "2026-07-17 05:58", "Atlanta", "GA"),
            stop(StopType.DEL, "2026-07-20 18:00", "Aurora", "CO"),
        )
        val base = sampleLoad(totalRate = 3019.11, stops = stops).withRouteMetrics()
        // Mimic EditLoad save: set finish date then recompute metrics.
        val edited = base.copy(actualFinishDate = "2026-07-18").withRouteMetrics()
        assertEquals(2.0, edited.durationDays, 0.001)
        assertTrue(edited.pace > base.pace)
    }

    private fun stop(
        type: StopType,
        scheduledTime: String,
        city: String,
        state: String,
    ) = Stop(
        id = type.name.hashCode() + scheduledTime.hashCode(),
        loadId = "id-1",
        stopNumber = 1,
        type = type,
        puNumber = null,
        note = null,
        scheduledTime = scheduledTime,
        timezone = "EDT",
        facilityCode = "",
        fullAddress = "$city, $state",
        city = city,
        state = state,
        zip = "",
    )

    private fun sampleLoad(
        durationDays: Double = 0.0,
        totalRate: Double = 500.0,
        stops: List<Stop> = emptyList(),
    ): Load = Load(
        id = "id-1",
        tripId = "T-1",
        date = "2026-07-17",
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
        durationDays = durationDays,
        stops = stops,
    )
}
