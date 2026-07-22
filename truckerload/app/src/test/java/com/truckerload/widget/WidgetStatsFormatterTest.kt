package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStatsFormatterTest {

    @Test
    fun formatGrossUsd_zeroAndLarge() {
        assertEquals("$0", WidgetStatsFormatter.formatGrossUsd(0.0))
        assertEquals("$1,234", WidgetStatsFormatter.formatGrossUsd(1234.4))
    }

    @Test
    fun formatCpm_twoDecimals() {
        assertEquals("2.46$", WidgetStatsFormatter.formatCpm(2.456))
        assertEquals("0.00$", WidgetStatsFormatter.formatCpm(0.0))
    }

    @Test
    fun formatMilesShort_thousandsSeparator() {
        assertEquals("1,250", WidgetStatsFormatter.formatMilesShort(1250.4))
        assertEquals("0", WidgetStatsFormatter.formatMilesShort(0.0))
    }

    @Test
    fun formatProgressPercent_clampsTo0_100() {
        assertEquals("0.0%", WidgetStatsFormatter.formatProgressPercent(-5f))
        assertEquals("100.0%", WidgetStatsFormatter.formatProgressPercent(150f))
        assertEquals("42.5%", WidgetStatsFormatter.formatProgressPercent(42.5f))
    }

    @Test
    fun avgRpm_zeroMilesSafe() {
        assertEquals(0.0, WidgetStatsFormatter.avgRpm(2500.0, 0.0), 0.0)
        assertEquals(2.5, WidgetStatsFormatter.avgRpm(2500.0, 1000.0), 0.001)
    }

    @Test
    fun formatDailyPaceShort_zeroAndFractional() {
        assertEquals("$0/d", WidgetStatsFormatter.formatDailyPaceShort(0.0))
        assertEquals("$0/d", WidgetStatsFormatter.formatDailyPaceShort(-10.0))
        assertEquals("$607/d", WidgetStatsFormatter.formatDailyPaceShort(607.0))
        assertTrue(WidgetStatsFormatter.formatDailyPaceShort(607.5).startsWith("$607.50"))
    }

    @Test
    fun formatRpmPerMile_legacyDollarFormat() {
        assertEquals("$2.46/mi", WidgetStatsFormatter.formatRpmPerMile(2.46))
    }

    @Test
    fun formatMiles_usesEnglishUnitSuffix() {
        assertEquals("1,250 mi", WidgetStatsFormatter.formatMiles(1250.4))
    }

    @Test
    fun formatDailyPace_usesEnglishDaySuffix() {
        assertEquals("$0/day", WidgetStatsFormatter.formatDailyPace(0.0))
        assertEquals("$607/day", WidgetStatsFormatter.formatDailyPace(607.0))
        assertTrue(WidgetStatsFormatter.formatDailyPace(607.5).endsWith("/day"))
    }
}
