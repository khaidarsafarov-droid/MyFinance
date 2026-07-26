package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseUtilsReceiptDateTest {

    @Test
    fun normalizeTextMonthDate_jul15() {
        assertEquals("2026-07-15", ParseUtils.normalizeTextMonthDate("Jul 15, 2026"))
        assertEquals("2026-07-15", ParseUtils.normalizeTextMonthDate("July 15 2026"))
        assertEquals("2026-07-15", ParseUtils.normalizeTextMonthDate("15 Jul 2026"))
    }

    @Test
    fun normalizeDate_dayFirstWhenDayGt12() {
        assertEquals("2026-07-15", ParseUtils.normalizeDate("15/07/2026"))
        assertEquals("2026-07-15", ParseUtils.normalizeDate("15-07-26"))
    }
}
