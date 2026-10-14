package com.truckerload.domain.model

import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.utils.getMillisForWeek
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice

class DieselJournalFilterTest {

    @Test
    fun forWeek_keepsMatchingWeekOnly() {
        val week35 = getMillisForWeek(35, 2026)
        val week34 = getMillisForWeek(34, 2026)
        val fills = listOf(
            sample(id = 1, week = 35, year = 2026, addedAt = week35),
            sample(id = 2, week = 34, year = 2026, addedAt = week34),
            sample(id = 3, week = 35, year = 2026, addedAt = week35 + 3_600_000L),
        )
        assertEquals(listOf(3, 1), DieselJournalFilter.forWeek(fills, 35, 2026).map { it.id })
    }

    @Test
    fun forWeek_keepsOneSpreadsheetInTheDieselWeekOfItsFirstFill() {
        val previous = WeekStartRuntime.diesel
        WeekStartRuntime.installDiesel(WeekStartDay.SUNDAY)
        try {
            val monday = millis(2026, Calendar.SEPTEMBER, 28)
            val nextSunday = millis(2026, Calendar.OCTOBER, 4)
            val (week, year) = getWeekNumberAndYearFromTimestamp(monday, WeekStartDay.SUNDAY)
            val (laterWeek, laterYear) = getWeekNumberAndYearFromTimestamp(nextSunday, WeekStartDay.SUNDAY)
            val fills = listOf(
                sample(id = 1, week = week, year = year, addedAt = monday, sourceFileName = "diesel_7299.xlsx"),
                sample(id = 2, week = week, year = year, addedAt = nextSunday, sourceFileName = "diesel_7299.xlsx"),
            )
            assertEquals(listOf(2, 1), DieselJournalFilter.forWeek(fills, week, year).map { it.id })
            assertTrue(DieselJournalFilter.forWeek(fills, laterWeek, laterYear).isEmpty())
        } finally {
            WeekStartRuntime.installDiesel(previous)
        }
    }

    @Test
    fun forDate_keepsMatchingCalendarDay() {
        val dayA = 1_777_046_400_000L // 2026-05-25 00:00 UTC; local day still 2026-05-25 in US/EU
        val dayB = dayA + 86_400_000L
        val fills = listOf(
            sample(id = 1, week = 22, year = 2026, addedAt = dayA + 3_600_000L),
            sample(id = 2, week = 22, year = 2026, addedAt = dayB + 3_600_000L),
        )
        val iso = com.truckerload.utils.formatIsoDate(dayA + 3_600_000L)
        assertEquals(listOf(1), DieselJournalFilter.forDate(fills, iso).map { it.id })
    }

    private fun millis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(Locale.US)
        cal.clear()
        cal.set(year, month, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun sample(
        id: Int,
        week: Int,
        year: Int,
        addedAt: Long,
        sourceFileName: String? = null,
    ) = Diesel(
        id = id,
        weekNumber = week,
        year = year,
        weekLabel = "W$week",
        weekStartDate = "2026-01-01",
        weekEndDate = "2026-01-07",
        totalAmount = Money.fromDollars(100.0),
        gallons = 20.0,
        pricePerGallon = UnitPrice.fromDollars(5.0),
        discountPricePerGallon = UnitPrice.fromDollars(4.0),
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = sourceFileName,
        addedAt = addedAt,
    )
}
