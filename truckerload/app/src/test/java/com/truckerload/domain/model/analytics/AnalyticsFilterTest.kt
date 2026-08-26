package com.truckerload.domain.model.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsFilterTest {

    @Test
    fun default_isLast12WeeksPreset() {
        val filter = AnalyticsFilter.DEFAULT
        assertEquals(AnalyticsPeriod.LAST_12_WEEKS, filter.preset)
        assertFalse(filter.isCalendar)
        assertEquals("LAST_12_WEEKS", filter.exportKey())
    }

    @Test
    fun selectYear_clearsPresetAndFinerLevels() {
        val filter = AnalyticsFilter.preset(AnalyticsPeriod.ALL_TIME)
            .selectYear(2026)
            .selectMonth(8)
            .selectWeek(34, 2026)
            .selectYear(2025)

        assertTrue(filter.isCalendar)
        assertNull(filter.preset)
        assertEquals(2025, filter.year)
        assertNull(filter.month)
        assertNull(filter.weekNumber)
        assertEquals("Y2025", filter.exportKey())
    }

    @Test
    fun selectMonth_togglesBackToWholeYear() {
        val year = AnalyticsFilter.DEFAULT.selectYear(2026)
        val august = year.selectMonth(8)
        assertEquals(8, august.month)

        val backToYear = august.selectMonth(8)
        assertEquals(2026, backToYear.year)
        assertNull(backToYear.month)
        assertNull(backToYear.weekNumber)
    }

    @Test
    fun selectWeek_togglesBackToWholeMonth() {
        val month = AnalyticsFilter.DEFAULT.selectYear(2026).selectMonth(8)
        val week = month.selectWeek(34, 2026)
        assertEquals(34, week.weekNumber)
        assertEquals(2026, week.weekYear)
        assertEquals("Y2026_M8_W34_2026", week.exportKey())

        val backToMonth = week.selectWeek(34, 2026)
        assertEquals(8, backToMonth.month)
        assertNull(backToMonth.weekNumber)
        assertEquals("Y2026_M8", backToMonth.exportKey())
    }

    @Test
    fun selectPreset_clearsCalendar() {
        val calendar = AnalyticsFilter.DEFAULT.selectYear(2025).selectMonth(2)
        val preset = calendar.selectPreset(AnalyticsPeriod.LAST_6_MONTHS)
        assertEquals(AnalyticsPeriod.LAST_6_MONTHS, preset.preset)
        assertFalse(preset.isCalendar)
        assertNull(preset.year)
    }

    @Test
    fun selectMonth_withoutYear_isNoOp() {
        val filter = AnalyticsFilter.DEFAULT.selectMonth(3)
        assertEquals(AnalyticsFilter.DEFAULT, filter)
    }
}
