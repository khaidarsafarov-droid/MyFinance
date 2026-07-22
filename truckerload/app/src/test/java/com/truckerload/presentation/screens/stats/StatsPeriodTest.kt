package com.truckerload.presentation.screens.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsPeriodTest {

    @Test
    fun entries_coverWeekMonthYear() {
        assertEquals(
            listOf(StatsPeriod.WEEK, StatsPeriod.MONTH, StatsPeriod.YEAR),
            StatsPeriod.entries,
        )
    }

    @Test
    fun labelFor_isExhaustiveOverAllPeriods() {
        StatsPeriod.entries.forEach { period ->
            assertEquals(expectedLabel(period), labelFor(period))
        }
    }

    private fun labelFor(period: StatsPeriod): String = when (period) {
        StatsPeriod.WEEK -> "week"
        StatsPeriod.MONTH -> "month"
        StatsPeriod.YEAR -> "year"
    }

    private fun expectedLabel(period: StatsPeriod): String = when (period) {
        StatsPeriod.WEEK -> "week"
        StatsPeriod.MONTH -> "month"
        StatsPeriod.YEAR -> "year"
    }
}
