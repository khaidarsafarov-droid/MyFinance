package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.parser.LoadMessageParser
import com.truckerload.domain.parser.ParseUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Regression: Relay Pu-time `MM/DD` must follow the Telegram message date, not device "now".
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
    }

    @Test
    fun normalizeDate_usesReferenceNotBookingHorizon() {
        val ref = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(
            "2026-08-21",
            ParseUtils.normalizeDate("08/21 01:39 EDT", defaultYear = 2026, referenceMillis = ref),
        )
    }

    @Test
    fun multiDayTrip_lastDelUsesMessageYearOnCardDate() {
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
                Stop(1, "114HZ2QZK", 1, StopType.PU, "PU1", null, "08/20 08:00 EDT", "EDT", null, "Burlington", "NJ", ""),
                Stop(2, "114HZ2QZK", 2, StopType.DEL, null, null, "08/21 09:00 EDT", "EDT", null, "Middletown", "PA", ""),
            ),
        )
        val repaired = LoadDateRepair.repair(load, anchorYearHint = 2025, referenceMillis = messageMillis)
        assertEquals("2025-08-20", repaired.date)
        assertEquals("2025-08-21", repaired.effectiveFinishDate())
    }
}
