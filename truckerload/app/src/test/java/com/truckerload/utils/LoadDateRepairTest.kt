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
    fun resolveRelayYear_futureMonthUsesPreviousYearWhenReferenceIsSpring() {
        // Reference = March 15 2026; Relay Pu-time 11/20 without year → Nov 2025
        val ref = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2025, LoadDateRepair.resolveRelayYear(11, 20, ref))
    }

    @Test
    fun resolveRelayYear_pastOrNearMonthKeepsReferenceYear() {
        val ref = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 26, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2026, LoadDateRepair.resolveRelayYear(7, 5, ref))
        assertEquals(2026, LoadDateRepair.resolveRelayYear(3, 1, ref))
        assertEquals(2026, LoadDateRepair.resolveRelayYear(8, 2, ref))
    }

    @Test
    fun resolveRelayYear_nearFutureAugustFromLateJulyKeepsReferenceYear() {
        // Late July booking for mid-August must stay in the same calendar year.
        val ref = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2026, LoadDateRepair.resolveRelayYear(8, 15, ref))
        assertEquals(2026, LoadDateRepair.resolveRelayYear(8, 20, ref))
        assertEquals(2026, LoadDateRepair.resolveRelayYear(8, 21, ref))
    }

    @Test
    fun resolveRelayYear_anchorsToTelegramMessageDate() {
        val ref = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 21, 2, 9, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 21, ref))
        assertEquals(2025, LoadDateRepair.resolveRelayYear(8, 20, ref))
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
    fun repair_nearFutureAugustFromJulyMessageKeepsMessageYear() {
        // Parsed July 30 2026 for Pu-time 08/20 → near-term booking in 2026 (not prior year).
        val parsedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val load = sample(
            id = "T-AUG20",
            date = "2025-08-20", // previously mislabeled by booking-horizon
            pu = "08/20 08:00 EDT",
            del = "08/21 15:00 EDT",
            miles = 400.0,
        ).copy(parsedAt = parsedAt, year = 2025)

        val repaired = LoadDateRepair.repair(load)
        assertEquals("2026-08-20", repaired.date)
        assertEquals(2026, repaired.year)
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
        val messageMillis = Calendar.getInstance().apply {
            set(2025, Calendar.JULY, 5, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
            parsedAt = messageMillis,
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

        val repaired = LoadDateRepair.repair(load, anchorYearHint = 2025, referenceMillis = messageMillis)
        assertEquals("2025-07-05", repaired.date)
        assertEquals(2025, repaired.year)
        assertTrue(repaired.weekNumber > 0)
    }

    @Test
    fun yearTotals_shouldNotIncludeWrongYearAfterRepair() {
        val july2025 = Calendar.getInstance().apply {
            set(2025, Calendar.JULY, 5, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val july2026 = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 20, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val wrongYear = LoadDateRepair.repair(
            sample("T-A", "2026-07-05", "07/05 08:00 EDT", "07/06 08:00 EDT", miles = 800.0, parsedAt = july2025),
            anchorYearHint = 2025,
            referenceMillis = july2025,
        )
        val thisYear = LoadDateRepair.repair(
            sample("T-B", "2026-07-20", "07/20 08:00 EDT", "07/21 08:00 EDT", miles = 900.0, parsedAt = july2026),
            anchorYearHint = 2026,
            referenceMillis = july2026,
        )
        val loads = listOf(wrongYear, thisYear)
        val miles2026 = loads.filter { it.date.startsWith("2026") }.sumOf { it.totalMiles }
        val miles2025 = loads.filter { it.date.startsWith("2025") }.sumOf { it.totalMiles }
        assertEquals(900.0, miles2026, 0.01)
        assertEquals(800.0, miles2025, 0.01)
    }

    @Test
    fun repair_fixesMislabeledThisYearWhenParsedAtIsLastYear() {
        // Three Aug-2025 Telegram loads that showed up in "this week" (Aug 2026).
        val cases = listOf(
            Triple("T-112QX54Y8", "08/21 01:39 EDT", calendar(2025, Calendar.AUGUST, 20, 14, 16)),
            Triple("T-116C43HGC", "08/22 22:44 EDT", calendar(2025, Calendar.AUGUST, 22, 10, 5)),
            Triple("T-112VZ3TL2", "08/22 23:28 EDT", calendar(2025, Calendar.AUGUST, 22, 13, 36)),
        )
        for ((id, pu, parsedAt) in cases) {
            val wrong = sample(
                id = id,
                date = "2026-${pu.take(5).replace('/', '-')}",
                pu = pu,
                del = pu, // same day DEL for minimal fixture
                miles = 400.0,
                parsedAt = parsedAt,
            ).copy(year = 2026)
            val repaired = LoadDateRepair.repair(wrong, referenceMillis = parsedAt)
            assertEquals(id, "2025-${pu.substring(0, 5).replace('/', '-')}", repaired.date)
            assertEquals(id, 2025, repaired.year)
        }
    }

    @Test
    fun repair_doesNotOverrideStoredYearWhenParsedAtMissing() {
        val load = sample(
            id = "T-KEEP25",
            date = "2025-08-21",
            pu = "08/21 01:39 EDT",
            del = "08/21 15:00 EDT",
            miles = 400.0,
            parsedAt = 1L, // sentinel — not a real message time
        ).copy(year = 2025, weekNumber = 34)
        val repaired = LoadDateRepair.repair(load)
        assertEquals("2025-08-21", repaired.date)
        assertEquals(2025, repaired.year)
    }

    private fun calendar(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun sample(
        id: String,
        date: String,
        pu: String,
        del: String,
        miles: Double,
        parsedAt: Long = 1L,
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
        parsedAt = parsedAt,
        updatedAt = 1L,
        stops = listOf(
            Stop(1, id, 1, StopType.PU, "PU1", null, pu, "EDT", null, "A", "A", "TX", ""),
            Stop(2, id, 2, StopType.DEL, null, null, del, "EDT", null, "B", "B", "TX", ""),
        ),
    )
}
