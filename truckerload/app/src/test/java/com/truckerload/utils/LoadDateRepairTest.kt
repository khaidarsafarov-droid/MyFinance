package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LoadDateRepairTest {

    @Test
    fun resolveRelayYear_futureMonthUsesPreviousYear() {
        // "Now" = March 15 2026; Relay Pu-time 11/20 without year → Nov 2025
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2025, LoadDateRepair.resolveRelayYear(11, 20, 2026, now))
    }

    @Test
    fun resolveRelayYear_pastOrNearMonthKeepsAnchorYear() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 26, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2026, LoadDateRepair.resolveRelayYear(7, 5, 2026, now))
        assertEquals(2026, LoadDateRepair.resolveRelayYear(3, 1, 2026, now))
        // Near-term booking within ~2 weeks keeps current year.
        assertEquals(2026, LoadDateRepair.resolveRelayYear(8, 2, 2026, now))
    }

    @Test
    fun resolveRelayYear_farFutureMonthUsesPreviousYear() {
        // Late July: August 15+ must not become "next month" pears for 2025 history.
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 15, 2026, now))
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 20, 2026, now))
    }

    @Test
    fun resolveRelayYear_staysStableWhenWallClockEntersBookingWindow() {
        // Parsed in late July as previous-year history; must not flip to 2026 in early August.
        val parsedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 20, 2026, parsedAt))
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 21, 2026, parsedAt))
    }

    @Test
    fun repair_doesNotFlipYearWhenHydratedAfterBookingWindowOpens() {
        // Load was stored/parsed on July 30 with Pu-time 08/20 → year 2025.
        // Hydrate/repair on August 8 must keep 2025 (anchor = parsedAt, not wall clock).
        val parsedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val load = sample(
            id = "T-AUG20",
            date = "2025-08-20",
            pu = "08/20 08:00 EDT",
            del = "08/21 15:00 EDT",
            miles = 400.0,
        ).copy(parsedAt = parsedAt, year = 2025)

        val repaired = LoadDateRepair.repair(load)
        assertEquals("2025-08-20", repaired.date)
        assertEquals(2025, repaired.year)
    }

    @Test
    fun repair_liveAugustBookingKeepsCurrentYear() {
        // Message received August 8 about Pu-time 08/21 → near-term booking in 2026.
        val parsedAt = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 8, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val load = sample(
            id = "T-LIVE21",
            date = "2026-08-21",
            pu = "08/21 01:39 EDT",
            del = "08/21 15:00 EDT",
            miles = 425.0,
        ).copy(parsedAt = parsedAt, year = 2026)

        val repaired = LoadDateRepair.repair(load, anchorYearHint = 2026, referenceMillis = parsedAt)
        assertEquals("2026-08-21", repaired.date)
        assertEquals(2026, repaired.year)
    }

    @Test
    fun repair_usesMessageYearForMmDdStops() {
        // Stored wrongly as 2026, but Telegram message was from 2025.
        val load = Load(
            id = "T-OLD",
            tripId = "T-OLD",
            date = "2026-07-05",
            totalRate = 1500.0,
            totalMiles = 400.0,
            pointA = "Austin, TX",
            pointB = "Dallas, TX",
            puCount = 1,
            delCount = 1,
            weekNumber = 28,
            year = 2026,
            rawMessage = "",
            parsedAt = 1L,
            updatedAt = 1L,
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "T-OLD",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = "PU1",
                    note = null,
                    scheduledTime = "07/05 18:00 EDT",
                    timezone = "EDT",
                    facilityCode = null,
                    fullAddress = "Austin, TX",
                    city = "Austin",
                    state = "TX",
                    zip = "",
                ),
                Stop(
                    id = 2,
                    loadId = "T-OLD",
                    stopNumber = 2,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = "07/06 00:01 EDT",
                    timezone = "EDT",
                    facilityCode = null,
                    fullAddress = "Dallas, TX",
                    city = "Dallas",
                    state = "TX",
                    zip = "",
                ),
            ),
        )

        val repaired = LoadDateRepair.repair(load, anchorYearHint = 2025)
        assertEquals("2025-07-05", repaired.date)
        assertEquals(2025, repaired.year)
        assertTrue(repaired.weekNumber > 0)
    }

    @Test
    fun yearTotals_shouldNotIncludeWrongYearAfterRepair() {
        val wrongYear = LoadDateRepair.repair(
            sample("T-A", "2026-07-05", "07/05 08:00 EDT", "07/06 08:00 EDT", miles = 800.0),
            anchorYearHint = 2025,
        )
        val thisYear = LoadDateRepair.repair(
            sample("T-B", "2026-07-20", "07/20 08:00 EDT", "07/21 08:00 EDT", miles = 900.0),
            anchorYearHint = 2026,
        )
        val loads = listOf(wrongYear, thisYear)
        val miles2026 = loads.filter { it.date.startsWith("2026") }.sumOf { it.totalMiles }
        val miles2025 = loads.filter { it.date.startsWith("2025") }.sumOf { it.totalMiles }
        assertEquals(900.0, miles2026, 0.01)
        assertEquals(800.0, miles2025, 0.01)
    }

    private fun sample(
        id: String,
        date: String,
        pu: String,
        del: String,
        miles: Double,
    ) = Load(
        id = id,
        tripId = id,
        date = date,
        totalRate = 1000.0,
        totalMiles = miles,
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
            Stop(1, id, 1, StopType.PU, "PU1", null, pu, "EDT", null, "A", "A", "TX", ""),
            Stop(2, id, 2, StopType.DEL, null, null, del, "EDT", null, "B", "B", "TX", ""),
        ),
    )
}
