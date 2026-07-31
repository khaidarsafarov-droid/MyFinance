package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ActualFinishDateTest {

    @Test
    fun normalize_dateOnlyAndDateTime() {
        assertEquals("2026-07-20", ActualFinishDate.normalize("2026-07-20"))
        assertEquals("2026-07-20 15:05", ActualFinishDate.normalize("2026-07-20 15:5"))
        assertEquals("2026-07-20 09:30", ActualFinishDate.normalize(" 2026-07-20 9:30 "))
        assertNull(ActualFinishDate.normalize(" "))
        assertNull(ActualFinishDate.normalize("nope"))
    }

    @Test
    fun toMillis_dateOnlyIsEndOfDay_datetimeIsExact() {
        val day = LocalDate.of(2026, 7, 20)
        val eod = day.atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val at1505 = day.atTime(15, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val dateOnlyMs = ActualFinishDate.toMillis("2026-07-20")
        assertNotNull(dateOnlyMs)
        assertTrue(dateOnlyMs!! >= eod - 1_000)
        assertEquals(at1505, ActualFinishDate.toMillis("2026-07-20 15:05"))
    }

    @Test
    fun hasTime_detectsClock() {
        assertFalse(ActualFinishDate.hasTime("2026-07-20"))
        assertTrue(ActualFinishDate.hasTime("2026-07-20 15:05"))
    }

    @Test
    fun combine_buildsCanonical() {
        assertEquals("2026-07-20 07:05", ActualFinishDate.combine("2026-07-20", 7, 5))
    }
}
