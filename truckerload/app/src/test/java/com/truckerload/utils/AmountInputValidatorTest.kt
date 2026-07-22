package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountInputValidatorTest {

    @Test
    fun parsePositiveAmount_acceptsValid() {
        assertEquals(100.0, AmountInputValidator.parsePositiveAmount("100")!!, 0.0)
        assertEquals(12.5, AmountInputValidator.parsePositiveAmount(" 12.5 ")!!, 0.0)
    }

    @Test
    fun parsePositiveAmount_rejectsBlankZeroNegativeNonNumeric() {
        assertNull(AmountInputValidator.parsePositiveAmount(""))
        assertNull(AmountInputValidator.parsePositiveAmount("   "))
        assertNull(AmountInputValidator.parsePositiveAmount("0"))
        assertNull(AmountInputValidator.parsePositiveAmount("-5"))
        assertNull(AmountInputValidator.parsePositiveAmount("abc"))
        assertFalse(AmountInputValidator.isValidPositiveAmount("0"))
        assertTrue(AmountInputValidator.isValidPositiveAmount("1"))
    }
}
