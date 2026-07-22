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
    fun dateStringToStartOfDayMillis_doesNotThrowOnBadInput() {
        val millis = dateStringToStartOfDayMillis("invalid")
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
}
