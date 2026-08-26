package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ParseUtilsMoneyTest {

    @Test
    fun parseMoney_usThousandsAndCents() {
        assertEquals(2500.5, ParseUtils.parseMoney("2,500.50"), 0.001)
        assertEquals(2500.0, ParseUtils.parseMoney("$2,500"), 0.001)
    }

    @Test
    fun parseMoney_euDecimalComma() {
        // FIX: previously stripped commas → 250050
        assertEquals(2500.50, ParseUtils.parseMoney("2500,50"), 0.001)
        assertEquals(1234.56, ParseUtils.parseMoney("1.234,56"), 0.001)
    }

    @Test
    fun parseMoney_blankIsZero() {
        assertEquals(0.0, ParseUtils.parseMoney(null), 0.0)
        assertEquals(0.0, ParseUtils.parseMoney("  "), 0.0)
    }
}
