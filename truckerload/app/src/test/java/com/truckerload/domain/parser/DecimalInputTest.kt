package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalInputTest {

    private fun assertParses(expected: Double, raw: String) {
        assertEquals(raw, expected, DecimalInput.parse(raw)!!, 1e-9)
    }

    @Test
    fun usThousandsAndDecimals() {
        assertParses(2500.0, "2,500")
        assertParses(2500.5, "2,500.50")
        assertParses(1_234_567.0, "1,234,567")
        assertParses(120_000.0, "120,000")
    }

    @Test
    fun euAndDecimalComma() {
        assertParses(2500.5, "2.500,50")
        assertParses(25.5, "25,5")
        assertParses(1234.56, "1,234,56")
        assertParses(2.5, "2.5")
    }

    @Test
    fun currencySignsSpacesAndSign() {
        assertParses(1234.0, "$1,234")
        assertParses(2500.0, " 2 500 USD ")
        assertParses(-50.0, "-50")
        assertParses(0.0, "0")
    }

    @Test
    fun garbageIsRejected() {
        assertNull(DecimalInput.parse(null))
        assertNull(DecimalInput.parse(""))
        assertNull(DecimalInput.parse("abc"))
        assertNull(DecimalInput.parse("12a"))
        assertNull(DecimalInput.parse("1.2.3,4,5"))
        assertNull(DecimalInput.parse("-"))
    }
}
