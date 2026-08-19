package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.parser.LoadMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekUtilsTest {

    @Test
    fun `parseDateFromScheduledTime handles Relay US Del-time`() {
        // FIX: without trustDefaultYear, omitted referenceMillis uses wall-clock and can shift year
        assertEquals(
            "2025-07-06",
            parseDateFromScheduledTime("07/06 00:01 EDT", defaultYear = 2025, trustDefaultYear = true),
        )
        assertEquals(
            "2025-07-05",
            parseDateFromScheduledTime("07/05 18:30 CDT", defaultYear = 2025, trustDefaultYear = true),
        )
        assertEquals("2025-07-06", parseDateFromScheduledTime("2025-07-06 00:01 EDT"))
        // defaultYear without wall-clock: July 2025 must not become 2026 in August 2026
        assertEquals("2025-07-06", parseDateFromScheduledTime("07/06 00:01 EDT", defaultYear = 2025))
    }

    @Test
    fun `Sunday Del moves load to new trucking week`() {
        // 2025-07-05 = Saturday, 2025-07-06 = Sunday (start of new Sun–Sat week)
        val load = sampleLoad(
            date = "2025-07-05",
            puTime = "07/05 18:00 EDT",
            delTime = "07/06 00:01 EDT",
        )

        val puWeek = getWeekNumberAndYearFromDate("2025-07-05")
        val delWeek = getWeekNumberAndYearFromDate("2025-07-06")
        assertTrue(
            "Sunday must start a new week vs Saturday",
            weekSortKey(delWeek) > weekSortKey(puWeek),
        )

        assertEquals("2025-07-06", getDeliveryDate(load))
        assertEquals(delWeek, getLoadReportingWeek(load))
    }

    @Test
    fun `late December uses week-year of next calendar year`() {
        // Sun–Sat: 2025-12-28 … 2026-01-03 is week 1 of week-year 2026
        val dec28 = getWeekNumberAndYearFromDate("2025-12-28")
        val jan1 = getWeekNumberAndYearFromDate("2026-01-01")
        assertEquals(1, dec28.first)
        assertEquals(2026, dec28.second)
        assertEquals(dec28, jan1)
        val (start, end, _) = getWeekRange(1, 2026)
        assertEquals("2025-12-28", start)
        assertEquals("2026-01-03", end)
    }

    @Test
    fun `New Year Relay trip keeps multi-day duration`() {
        val load = sampleLoad(
            date = "2025-12-30",
            puTime = "12/30 08:00 EST",
            delTime = "01/02 18:00 EST",
        )
        val start = getFirstPickUpMillis(load)
        val end = getLastDeliveryMillis(load)
        assertNotNull(start)
        assertNotNull(end)
        assertTrue("DEL must be after PU across year boundary", end!! > start!!)
    }

    @Test
    fun `same-week Del keeps PU week`() {
        val load = sampleLoad(
            date = "2025-07-01",
            puTime = "07/01 08:00 EDT",
            delTime = "07/03 09:00 EDT",
        )
        assertEquals(getWeekNumberAndYearFromDate("2025-07-01"), getLoadReportingWeek(load))
    }

    @Test
    fun `parser assigns reporting week from Sunday Del`() {
        // Use fixed ISO dates via year-aware Relay times relative to load.date year.
        // 2025-07-05 Sat / 2025-07-06 Sun 00:01
        val message = """
            Trip ID: T-SUNDAYDEL
            Total Rate: ${'$'}1500.00
            Total Loaded Miles: 400 mi
            PU# PU1
            Pu-time: 07/05 18:00 EDT
            Pu-address: Austin, TX 78701
            Del-time: 07/06 00:01 EDT
            Del-address: Dallas, TX 75201
        """.trimIndent()

        val load = LoadMessageParser.parseOne(message)!!
        // Force year of stops via withReportingWeek on a copy with known date year.
        val anchored = load.copy(date = "2025-07-05").withReportingWeek()
        val delWeek = getWeekNumberAndYearFromDate("2025-07-06")
        assertEquals("2025-07-06", getDeliveryDate(anchored))
        assertEquals(delWeek.first, anchored.weekNumber)
        assertEquals(delWeek.second, anchored.year)
        assertNotNull(getDeliveryDate(load))
    }

    @Test
    fun spanningWeekBelongsToSaturdayMonthOnly() {
        // Sun 2025-01-26 … Sat 2025-02-01
        val spanning = getWeekNumberAndYearFromDate("2025-01-26")
        assertEquals(spanning, getWeekNumberAndYearFromDate("2025-02-01"))
        val (_, end, _) = getWeekRange(spanning.first, spanning.second)
        assertEquals("2025-02-01", end)
        assertFalse(getWeeksInMonth(2025, 1).contains(spanning))
        assertTrue(getWeeksInMonth(2025, 2).contains(spanning))
    }

    @Test
    fun decemberWeek1OfNextYearBelongsToJanuary() {
        val week1 = getWeekNumberAndYearFromDate("2025-12-28")
        assertEquals(1, week1.first)
        assertEquals(2026, week1.second)
        assertFalse(getWeeksInMonth(2025, 12).contains(week1))
        assertTrue(getWeeksInMonth(2026, 1).contains(week1))
        val year2025 = weeksEndingInRange("2025-01-01", "2025-12-31")
        val year2026 = weeksEndingInRange("2026-01-01", "2026-12-31")
        assertFalse(year2025.contains(week1))
        assertTrue(year2026.contains(week1))
    }

    @Test
    fun monthsDoNotShareReportingWeeks() {
        val jan = getWeeksInMonth(2025, 1)
        val feb = getWeeksInMonth(2025, 2)
        assertTrue(jan.intersect(feb.toSet()).isEmpty())
        val yearWeeks = weeksEndingInRange("2025-01-01", "2025-12-31")
        val monthWeeks = (1..12).flatMap { getWeeksInMonth(2025, it) }
        assertEquals(yearWeeks.toSet(), monthWeeks.toSet())
        assertEquals(yearWeeks.size, monthWeeks.size)
    }

    private fun weekSortKey(week: Pair<Int, Int>): Long = week.second * 100L + week.first

    private fun sampleLoad(date: String, puTime: String, delTime: String): Load =
        Load(
            id = "id",
            tripId = "T-TEST",
            date = date,
            totalRate = 1000.0,
            totalMiles = 100.0,
            pointA = "A",
            pointB = "B",
            puCount = 1,
            delCount = 1,
            weekNumber = 0,
            year = 0,
            rawMessage = "",
            parsedAt = 0L,
            updatedAt = 0L,
            stops = listOf(
                Stop(
                    id = 1,
                    loadId = "id",
                    stopNumber = 1,
                    type = StopType.PU,
                    puNumber = "PU1",
                    note = null,
                    scheduledTime = puTime,
                    timezone = "EDT",
                    facilityCode = null,
                    fullAddress = "Austin, TX",
                    city = "Austin",
                    state = "TX",
                    zip = "78701",
                ),
                Stop(
                    id = 2,
                    loadId = "id",
                    stopNumber = 2,
                    type = StopType.DEL,
                    puNumber = null,
                    note = null,
                    scheduledTime = delTime,
                    timezone = "EDT",
                    facilityCode = null,
                    fullAddress = "Dallas, TX",
                    city = "Dallas",
                    state = "TX",
                    zip = "75201",
                ),
            ),
        )
}
