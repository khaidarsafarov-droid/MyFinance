package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WeekUtilsDateParsingTest {

    @Test
    fun parseIsoDateParts_validDate() {
        assertEquals(Triple(2026, 7, 16), parseIsoDateParts("2026-07-16"))
    }

    @Test
    fun parseIsoDateParts_rejectsGarbage() {
        assertNull(parseIsoDateParts("not-a-date"))
        assertNull(parseIsoDateParts("2026-13-40"))
        assertNull(parseIsoDateParts("2026-07-xx"))
    }

@Test
    fun dateStringToStartOfDayMillis_returnsNullOnBadInput() {
        assertNull(dateStringToStartOfDayMillis("invalid"))
        assertNull(dateStringToEndOfDayMillis("2026-13-99"))
    }

    @Test
    fun dateStringToStartOfDayMillis_parsesValidDate() {
        val millis = dateStringToStartOfDayMillis("2026-07-16")
        assertNotNull(millis)
    }

    @Test
    fun datePickerUtcRoundTrip_preservesCalendarDay() {
        val iso = "2026-07-17"
        val utcMs = dateStringToUtcDatePickerMillis(iso)
        assertNotNull(utcMs)
        assertEquals(iso, utcDatePickerMillisToDateString(utcMs!!))
    }

    @Test
    fun datePickerUtc_doesNotShiftDayWhenConvertedViaUtc() {
        // Regression: local Calendar on UTC midnight used to yield previous day in US timezones.
        val utcMs = dateStringToUtcDatePickerMillis("2026-07-22")!!
        assertEquals("2026-07-22", utcDatePickerMillisToDateString(utcMs))
    }

    @Test
    fun parseScheduledTimeToMillis_relayYearAlignsWithResolveRelayYear() {
        val ref = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.MARCH, 15, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val defaultYear = 2026
        val millis = parseScheduledTimeToMillis("11/20 08:00 EDT", defaultYear = defaultYear, referenceMillis = ref)
        assertNotNull(millis)
        val expectedYear = LoadDateRepair.resolveRelayYear(11, 20, ref)
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis!!
        assertEquals(expectedYear, cal.get(java.util.Calendar.YEAR))
        assertEquals(11, cal.get(java.util.Calendar.MONTH) + 1)
        assertEquals(20, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(java.util.Calendar.HOUR_OF_DAY))

        val dateOnly = parseDateFromScheduledTime("11/20 08:00 EDT", defaultYear = defaultYear, referenceMillis = ref)
        assertEquals("%04d-11-20".format(expectedYear), dateOnly)
    }
}
