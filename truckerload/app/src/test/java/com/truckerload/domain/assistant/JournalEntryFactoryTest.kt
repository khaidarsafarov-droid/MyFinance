package com.truckerload.domain.assistant

import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class JournalEntryFactoryTest {

    @Test
    fun dieselUsesSpokenDateAndGallons() {
        val now = calendarMillis(2026, Calendar.AUGUST, 21, 15, 30)
        val diesel = JournalEntryFactory.diesel(
            amount = 80.0,
            gallons = 20.0,
            date = "2026-08-20",
            nowMillis = now,
        )
        assertNotNull(diesel)
        assertEquals(80.0, diesel!!.totalAmount, 0.0)
        assertEquals(20.0, diesel.gallons ?: 0.0, 0.0)
        assertEquals(4.0, diesel.pricePerGallon ?: 0.0, 0.0)
        val (week, year) = getWeekNumberAndYearFromTimestamp(diesel.addedAt)
        assertEquals(week, diesel.weekNumber)
        assertEquals(year, diesel.year)
        val added = Calendar.getInstance().apply { timeInMillis = diesel.addedAt }
        assertEquals(20, added.get(Calendar.DAY_OF_MONTH))
        assertEquals(15, added.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun dieselRejectsNonPositiveAmount() {
        assertNull(JournalEntryFactory.diesel(amount = 0.0, gallons = 10.0, date = null))
        assertNull(JournalEntryFactory.diesel(amount = -5.0, gallons = null, date = null))
    }

    @Test
    fun paycheckDefaultsToCurrentWeek() {
        val now = calendarMillis(2026, Calendar.AUGUST, 21, 10, 0)
        val paycheck = JournalEntryFactory.paycheck(
            amount = 2500.0,
            weekNumber = null,
            year = null,
            nowMillis = now,
        )
        assertNotNull(paycheck)
        val (week, year) = getWeekNumberAndYearFromTimestamp(now)
        assertEquals(week, paycheck!!.weekNumber)
        assertEquals(year, paycheck.year)
        assertEquals(2500.0, paycheck.netAmount, 0.0)
    }

    @Test
    fun paycheckHonorsExplicitWeek() {
        val paycheck = JournalEntryFactory.paycheck(
            amount = 1000.0,
            weekNumber = 3,
            year = 2026,
            nowMillis = calendarMillis(2026, Calendar.AUGUST, 21, 10, 0),
        )
        assertEquals(3, paycheck!!.weekNumber)
        assertEquals(2026, paycheck.year)
    }

    private fun calendarMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(Locale.US).apply {
            clear()
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
