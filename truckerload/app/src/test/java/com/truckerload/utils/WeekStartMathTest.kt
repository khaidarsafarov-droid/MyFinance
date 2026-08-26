package com.truckerload.utils

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRebinder
import com.truckerload.domain.week.WeekStartRuntime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class WeekStartMathTest {

    @Before
    fun resetRuntime() {
        WeekStartRuntime.install(WeekStartDay.DEFAULT, WeekStartDay.DEFAULT)
    }

    @After
    fun resetRuntimeAfter() {
        WeekStartRuntime.install(WeekStartDay.DEFAULT, WeekStartDay.DEFAULT)
    }

    @Test
    fun sundayStart_lateDecemberIsWeek1OfNextYear() {
        val dec28 = getWeekNumberAndYearFromDate("2025-12-28", WeekStartDay.SUNDAY)
        assertEquals(1, dec28.first)
        assertEquals(2026, dec28.second)
        val (start, end, _) = getWeekRange(1, 2026, WeekStartDay.SUNDAY)
        assertEquals("2025-12-28", start)
        assertEquals("2026-01-03", end)
    }

    @Test
    fun mondayStart_sundayBelongsToPreviousMondayWeek() {
        val sunday = getWeekNumberAndYearFromDate("2025-07-06", WeekStartDay.MONDAY)
        val monday = getWeekNumberAndYearFromDate("2025-06-30", WeekStartDay.MONDAY)
        assertEquals(monday, sunday)
        val (start, end, _) = getWeekRange(sunday.first, sunday.second, WeekStartDay.MONDAY)
        assertEquals("2025-06-30", start)
        assertEquals("2025-07-06", end)
        assertNotEquals(
            getWeekNumberAndYearFromDate("2025-07-06", WeekStartDay.SUNDAY),
            sunday,
        )
    }

    @Test
    fun loadsAndDieselStartsAreIndependent() {
        WeekStartRuntime.install(WeekStartDay.MONDAY, WeekStartDay.SUNDAY)
        val date = "2025-07-06"
        assertEquals(
            getWeekNumberAndYearFromDate(date, WeekStartDay.MONDAY),
            getWeekNumberAndYearFromDate(date, WeekStartRuntime.loads),
        )
        assertEquals(
            getWeekNumberAndYearFromDate(date, WeekStartDay.SUNDAY),
            getWeekNumberAndYearFromDate(date, WeekStartRuntime.diesel),
        )
        assertNotEquals(
            getWeekNumberAndYearFromDate(date, WeekStartRuntime.loads),
            getWeekNumberAndYearFromDate(date, WeekStartRuntime.diesel),
        )
    }

    @Test
    fun daysElapsed_wednesdayIsFourthOnSundayStartAndThirdOnMondayStart() {
        val wednesday = millis(2025, Calendar.JULY, 9)
        assertEquals(4, WeekCalendarMath.daysElapsed(WeekStartDay.SUNDAY, wednesday))
        assertEquals(3, WeekCalendarMath.daysElapsed(WeekStartDay.MONDAY, wednesday))
        assertEquals(4, WeekCalendarMath.daysRemaining(WeekStartDay.SUNDAY, wednesday))
        assertEquals(5, WeekCalendarMath.daysRemaining(WeekStartDay.MONDAY, wednesday))
    }

    @Test
    fun rebinderMovesDieselFillWhenDieselWeekStartChanges() {
        val sundayFill = millis(2025, Calendar.JULY, 6)
        val original = Diesel(
            id = 1,
            weekNumber = 0,
            year = 0,
            weekLabel = "",
            weekStartDate = "",
            weekEndDate = "",
            totalAmount = 50.0,
            gallons = 10.0,
            pricePerGallon = 5.0,
            location = null,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = sundayFill,
        )
        WeekStartRuntime.installDiesel(WeekStartDay.SUNDAY)
        val sundayWeek = WeekStartRebinder.recomputeDiesel(listOf(original)).single()
        WeekStartRuntime.installDiesel(WeekStartDay.MONDAY)
        val mondayWeek = WeekStartRebinder.recomputeDiesel(listOf(sundayWeek)).single()
        assertEquals("2025-07-06", sundayWeek.weekStartDate)
        assertEquals("2025-06-30", mondayWeek.weekStartDate)
        assertEquals(sundayFill, mondayWeek.addedAt)
        assertTrue(mondayWeek.weekNumber != sundayWeek.weekNumber || mondayWeek.year != sundayWeek.year)
    }

    @Test
    fun endCalendarDayWrapsFromMondayToSunday() {
        assertEquals(Calendar.SATURDAY, WeekStartDay.SUNDAY.endCalendarDay)
        assertEquals(Calendar.SUNDAY, WeekStartDay.MONDAY.endCalendarDay)
        assertEquals(Calendar.MONDAY, WeekStartDay.TUESDAY.endCalendarDay)
    }

    private fun millis(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance(Locale.US)
        cal.clear()
        cal.set(year, month, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
