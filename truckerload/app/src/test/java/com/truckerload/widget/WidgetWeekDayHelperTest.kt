package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WidgetWeekDayHelperTest {

    private val weekStart = LocalDate.of(2026, 8, 16) // Sunday
    private val wednesday = LocalDate.of(2026, 8, 19)

    @Test
    fun maskFromIsoDates_setsBitsForSundayThroughSaturday() {
        val mask = WidgetWeekDayHelper.maskFromIsoDates(
            listOf("2026-08-16", "2026-08-18T08:00", "2026-08-22"),
            weekStart,
        )
        assertEquals(
            (1 shl 0) or (1 shl 2) or (1 shl 6),
            mask,
        )
    }

    @Test
    fun maskFromIsoDates_ignoresDatesOutsideTheWeekAndJunk() {
        val mask = WidgetWeekDayHelper.maskFromIsoDates(
            listOf("2026-08-15", "nope", "2026-08-23", "2026-08-17"),
            weekStart,
        )
        assertEquals(1 shl 1, mask)
    }

    @Test
    fun chips_independentHasLoadAndTodaySignals() {
        val mask = (1 shl 0) or (1 shl 3) // Sunday + Wednesday
        val chips = WidgetWeekDayHelper.chips(mask, wednesday, weekStart)

        assertEquals(listOf("S", "M", "T", "W", "T", "F", "S"), chips.map { it.label })

        assertTrue(chips[0].hasLoad)
        assertTrue(chips[0].isPast)
        assertFalse(chips[0].isToday)

        assertFalse(chips[1].hasLoad)
        assertTrue(chips[1].isPast)

        assertTrue(chips[3].hasLoad)
        assertTrue(chips[3].isToday)
        assertFalse(chips[3].isFuture)
        assertFalse(chips[3].isPast)

        assertFalse(chips[4].hasLoad)
        assertTrue(chips[4].isFuture)
        assertFalse(chips[4].isToday)
    }

    @Test
    fun chips_todayWithoutLoadStaysUnfilled() {
        val chips = WidgetWeekDayHelper.chips(weekLoadMask = 0, today = wednesday, weekStart = weekStart)
        val today = chips.single { it.isToday }
        assertEquals("W", today.label)
        assertFalse(today.hasLoad)
        assertFalse(today.isPast)
        assertFalse(today.isFuture)
    }

    @Test
    fun chips_mondayWeekStartRotatesLabels() {
        val monday = LocalDate.of(2026, 8, 17)
        val chips = WidgetWeekDayHelper.chips(weekLoadMask = 0, today = wednesday, weekStart = monday)
        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), chips.map { it.label })
        assertEquals(monday, chips.first().date)
        assertEquals(LocalDate.of(2026, 8, 23), chips.last().date)
        assertTrue(chips[2].isToday)
    }
}
