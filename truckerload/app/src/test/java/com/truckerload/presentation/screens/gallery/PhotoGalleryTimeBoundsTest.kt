package com.truckerload.presentation.screens.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PhotoGalleryTimeBoundsTest {

    private fun cal(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun startOfDay_isMidnight() {
        val now = cal(2026, Calendar.JULY, 22, 15, 30)
        val start = PhotoGalleryTimeBounds.startOfDay(now)
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = start }
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(22, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun endOfDay_isLastMillis() {
        val now = cal(2026, Calendar.JULY, 22, 8, 0)
        val end = PhotoGalleryTimeBounds.endOfDay(now)
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = end }
        assertEquals(23, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, c.get(Calendar.MINUTE))
        assertEquals(999, c.get(Calendar.MILLISECOND))
    }

    @Test
    fun isInToday_excludesYesterdayAndTomorrow() {
        val now = cal(2026, Calendar.JULY, 22, 12, 0)
        val todayNoon = now.timeInMillis
        val yesterday = cal(2026, Calendar.JULY, 21, 23, 59).timeInMillis
        val tomorrow = cal(2026, Calendar.JULY, 23, 0, 1).timeInMillis
        assertTrue(PhotoGalleryTimeBounds.isInToday(todayNoon, now))
        assertFalse(PhotoGalleryTimeBounds.isInToday(yesterday, now))
        assertFalse(PhotoGalleryTimeBounds.isInToday(tomorrow, now))
    }

    @Test
    fun isInThisWeek_includesWeekStartExcludesPriorDay() {
        // Wednesday Jul 22 2026; firstDayOfWeek = Sunday → week starts Jul 19
        val now = cal(2026, Calendar.JULY, 22, 12, 0)
        val weekStart = PhotoGalleryTimeBounds.startOfWeek(now)
        val beforeWeek = weekStart - 1
        assertTrue(PhotoGalleryTimeBounds.isInThisWeek(weekStart, now))
        assertTrue(PhotoGalleryTimeBounds.isInThisWeek(now.timeInMillis, now))
        assertFalse(PhotoGalleryTimeBounds.isInThisWeek(beforeWeek, now))
    }
}
