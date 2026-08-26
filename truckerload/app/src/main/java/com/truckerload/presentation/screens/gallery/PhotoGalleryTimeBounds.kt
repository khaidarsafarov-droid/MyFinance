package com.truckerload.presentation.screens.gallery

import com.truckerload.domain.week.WeekStartRuntime
import java.util.Calendar

/** Pure calendar bounds for gallery TODAY / THIS_WEEK filters. */
object PhotoGalleryTimeBounds {

    fun startOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun endOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 23)
        c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59)
        c.set(Calendar.MILLISECOND, 999)
        return c.timeInMillis
    }

    fun startOfWeek(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        val firstDay = WeekStartRuntime.loads.calendarDay
        c.firstDayOfWeek = firstDay
        c.minimalDaysInFirstWeek = 1
        // Subtract days to week start. Setting DAY_OF_WEEK alone can roll forward.
        val dayOfWeek = c.get(Calendar.DAY_OF_WEEK)
        val daysFromWeekStart = (dayOfWeek - firstDay + 7) % 7
        c.add(Calendar.DAY_OF_MONTH, -daysFromWeekStart)
        return startOfDay(c)
    }

    fun isInToday(timestamp: Long, now: Calendar = Calendar.getInstance()): Boolean {
        val start = startOfDay(now)
        val end = endOfDay(now)
        return timestamp in start..end
    }

    fun isInThisWeek(timestamp: Long, now: Calendar = Calendar.getInstance()): Boolean =
        timestamp >= startOfWeek(now)
}
