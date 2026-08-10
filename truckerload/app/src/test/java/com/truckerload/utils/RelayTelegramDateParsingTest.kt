package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.model.lastDelDateFromStops
import com.truckerload.domain.parser.LoadMessageParser
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Regression: Relay Pu-time `MM/DD` must follow the Telegram message / stored load year,
 * not device wall-clock "now" when rendering journal cards.
 */
class RelayTelegramDateParsingTest {

    @Test
    fun telegramAugust21Message_parsesPuTimeAs2025() {
        val messageMillis = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 21, 2, 9, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val raw = """
            Trip ID: T-112QX54Y8
            Total Rate: ${'$'}1197.76
            Total Loaded Miles: 425 mi
            PU# 112Y7C36C
            Note: Empty trailer
            Pu-time: 08/21 01:39 EDT
            Pu-address: MDT5, 200 Goodman Dr
            LEWISBERRY, PA 17339
            Del-time: 08/21 03:32 EDT
            Del-address: VENDOR-165096271, 5197 COMMERCE DR
            YORK, PA 17408
        """.trimIndent()

        val parsed = LoadMessageParser.parseOne(raw)!!.copy(parsedAt = messageMillis)
        val repaired = LoadDateRepair.repair(
            parsed,
            anchorYearHint = 2025,
            referenceMillis = messageMillis,
        )

        assertEquals("2025-08-21", repaired.date)
        assertEquals(2025, repaired.year)
        assertEquals("2025-08-21", repaired.effectiveFinishDate())
        assertEquals("2025-08-21", repaired.lastDelDateFromStops())
    }

    @Test
    fun cardDate_trustsLoadDateYearNotWallClock() {
        // Even if device "now" is August 2026, a load stored as 2025 must show 2025 on the card.
        val load = Load(
            id = "114HZ2QZK",
            tripId = "114HZ2QZK",
            date = "2025-08-20",
            totalRate = 399.11,
            totalMiles = 113.0,
            pointA = "Burlington, NJ",
            pointB = "Middletown, PA",
            puCount = 1,
            delCount = 1,
            weekNumber = 34,
            year = 2025,
            rawMessage = "",
            parsedAt = 1_753_833_600_000L,
            updatedAt = 1_753_833_600_000L,
            stops = listOf(
                Stop(1, "114HZ2QZK", 1, StopType.PU, "PU1", null, "08/20 08:00 EDT", "EDT", null, "Burlington, NJ", "Burlington", "NJ", ""),
                Stop(2, "114HZ2QZK", 2, StopType.DEL, null, null, "08/21 09:00 EDT", "EDT", null, "Middletown, PA", "Middletown", "PA", ""),
            ),
        )
        assertEquals("2025-08-21", load.effectiveFinishDate())
        assertEquals("2025-08-21", load.lastDelDateFromStops())
    }

    @Test
    fun multiDayTrip_repairUsesMessageYear() {
        val messageMillis = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 20, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val load = Load(
            id = "114HZ2QZK",
            tripId = "114HZ2QZK",
            date = "2026-08-20",
            totalRate = 399.11,
            totalMiles = 113.0,
            pointA = "Burlington, NJ",
            pointB = "Middletown, PA",
            puCount = 1,
            delCount = 1,
            weekNumber = 1,
            year = 2026,
            rawMessage = "",
            parsedAt = messageMillis,
            updatedAt = messageMillis,
            stops = listOf(
                Stop(1, "114HZ2QZK", 1, StopType.PU, "PU1", null, "08/20 08:00 EDT", "EDT", null, "Burlington, NJ", "Burlington", "NJ", ""),
                Stop(2, "114HZ2QZK", 2, StopType.DEL, null, null, "08/21 09:00 EDT", "EDT", null, "Middletown, PA", "Middletown", "PA", ""),
            ),
        )
        val repaired = LoadDateRepair.repair(load, anchorYearHint = 2025, referenceMillis = messageMillis)
        assertEquals("2025-08-20", repaired.date)
        assertEquals("2025-08-21", repaired.effectiveFinishDate())
    }

    @Test
    fun newYearTrip_delBumpsToNextYearOnCard() {
        val load = Load(
            id = "T-NYE",
            tripId = "T-NYE",
            date = "2025-12-30",
            totalRate = 1000.0,
            totalMiles = 200.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 1,
            year = 2025,
            rawMessage = "",
            parsedAt = 1_766_000_000_000L,
            updatedAt = 1L,
            stops = listOf(
                Stop(1, "T-NYE", 1, StopType.PU, "PU1", null, "12/30 18:00 EST", "EST", null, "A", "A", "TX", ""),
                Stop(2, "T-NYE", 2, StopType.DEL, null, null, "01/02 08:00 EST", "EST", null, "B", "B", "TX", ""),
            ),
        )
        assertEquals("2026-01-02", load.lastDelDateFromStops())
        assertEquals("2026-01-02", load.effectiveFinishDate())
        assertEquals("2026-01-02", getDeliveryDate(load))
    }
}
