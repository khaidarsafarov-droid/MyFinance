package com.truckerload.utils

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.model.lastDelDateTimeFromStops
import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Relay stop times have no year; a trip over New Year must not put DEL a year before PU. */
class NewYearTripDatesTest {

    private fun stop(n: Int, type: StopType, time: String) = Stop(
        id = n, loadId = "id", stopNumber = n, type = type, puNumber = null, note = null,
        scheduledTime = time, timezone = "EST", facilityCode = null, fullAddress = "", city = "", state = "", zip = "",
    )

    private fun load(date: String, pu: String, del: String) = Load(
        id = "id", tripId = "T-NY", date = date, totalRate = Money.fromDollars(1000.0), totalMiles = 500.0,
        pointA = "A", pointB = "B", puCount = 1, delCount = 1, weekNumber = 0, year = 0,
        rawMessage = "", parsedAt = 0L, updatedAt = 0L,
        stops = listOf(stop(1, StopType.PU, pu), stop(2, StopType.DEL, del)),
    )

    @Test
    fun `delivery after New Year lands in the next year`() {
        val trip = load("2026-12-31", "12/31 08:00 EST", "01/03 18:00 EST")
        assertEquals("2027-01-03", getDeliveryDate(trip))
        assertEquals("2027-01-03", trip.effectiveFinishDate())
        assertEquals("2027-01-03 18:00", trip.lastDelDateTimeFromStops())
        assertTrue(getLastDeliveryMillis(trip)!! > getFirstPickUpMillis(trip)!!)
    }

    @Test
    fun `New Year trip is active only on its own days`() {
        val trip = load("2026-12-31", "12/31 08:00 EST", "01/03 18:00 EST")
        assertEquals(
            setOf("2026-12-31", "2027-01-01", "2027-01-02", "2027-01-03"),
            getLoadDateRange(trip),
        )
    }

    @Test
    fun `Sunday delivery after New Year moves the load to the delivery week`() {
        // Sun–Sat weeks: Thu 2026-12-31 is in week 1 of 2027, Sun 2027-01-03 starts week 2.
        val trip = load("2026-12-31", "12/31 08:00 EST", "01/03 18:00 EST")
        assertEquals(getWeekNumberAndYearFromDate("2027-01-03"), getLoadReportingWeek(trip))
    }

    @Test
    fun `pickup on New Year's Eve with load dated in January keeps December`() {
        assertEquals("2026-12-31", stopDateForLoad("12/31 22:00 EST", "2027-01-01"))
    }

    @Test
    fun `dated stops and same-year Relay stops are unchanged`() {
        assertEquals("2026-07-06", stopDateForLoad("07/06 00:01 EDT", "2026-07-05"))
        assertEquals("2025-01-02", stopDateForLoad("2025-01-02 08:00", "2026-12-31"))
    }

    @Test
    fun `a broken stop date cannot spread a load over months`() {
        val trip = load("2026-03-01", "03/01 08:00 EST", "2026-08-20 08:00")
        assertTrue(getLoadDateRange(trip).size <= 64)
        assertTrue("2026-08-20" in getLoadDateRange(trip))
    }
}
