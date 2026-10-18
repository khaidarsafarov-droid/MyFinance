package com.truckerload.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class WidgetSplitFormatTest {

    @Test
    fun sameMonth_printsOneMonthName() {
        val text = formatSplitWeekRange(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 18),
            Locale.US,
        )
        assertEquals("12 – 18 oct", text)
    }

    @Test
    fun acrossMonths_printsBothMonthNames() {
        val text = formatSplitWeekRange(
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 11, 1),
            Locale.US,
        )
        assertEquals("26 oct – 1 nov", text)
    }
}
