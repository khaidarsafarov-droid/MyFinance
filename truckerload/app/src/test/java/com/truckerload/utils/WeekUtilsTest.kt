package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.parser.LoadMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekUtilsTest {

    @Test
    fun `parseDateFromScheduledTime handles Relay US Del-time`() {
        assertEquals("2025-07-06", parseDateFromScheduledTime("07/06 00:01 EDT", defaultYear = 2025))
        assertEquals("2025-07-05", parseDateFromScheduledTime("07/05 18:30 CDT", defaultYear = 2025))
        assertEquals("2025-07-06", parseDateFromScheduledTime("2025-07-06 00:01 EDT"))
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
    fun `same-week Del keeps PU week`() {
        val load = sampleLoad(
            date = "2025-07-01",
            puTime = "07/01 08:00 EDT",
            delTime = "07/03 09:00 EDT",
        )
        assertEquals(getWeekNumberAndYearFromDate("2025-07-01"), getLoadReportingWeek(load))
    }

    @Test
    fun `yearBoundarySundayUsesWeekYearNotCivilYear`() {
        // 2025-12-28 is Sunday → trucking week 1 of week-year 2026
        val (week, year) = getWeekNumberAndYearFromDate("2025-12-28")
        assertEquals(1, week)
        assertEquals(2026, year)
        val (start, end, _) = getWeekRange(week, year)
        assertEquals("2025-12-28", start)
        assertEquals("2026-01-03", end)
    }

    @Test
    fun `parseScheduledTimeToMillisUsesLoadDateYearForRelay`() {
        val millis = parseScheduledTimeToMillis("07/05 18:00 EDT", defaultYear = 2025)
        assertNotNull(millis)
        val (w, y) = getWeekNumberAndYearFromTimestamp(millis!!)
        assertEquals(2025, y)
        assertTrue(w > 0)
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
