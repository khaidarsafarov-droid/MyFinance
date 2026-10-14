package com.truckerload.utils

import com.truckerload.R
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
    fun parsePositiveAmount_acceptsCommaDecimalAndThousands() {
        assertEquals(2750.25, AmountInputValidator.parsePositiveAmount("2 750,25")!!, 0.001)
        assertEquals(2500.5, AmountInputValidator.parsePositiveAmount("2,500.50")!!, 0.001)
        assertTrue(AmountInputValidator.isValidPositiveAmount("1.234,56"))
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

    @Test
    fun fieldErrors_nameTheProblem() {
        assertEquals(R.string.field_error_amount_example, FieldErrors.amountRes(""))
        assertEquals(R.string.field_error_amount_example, FieldErrors.amountRes("abc"))
        assertEquals(R.string.field_error_amount_example, FieldErrors.amountRes("0"))
        assertNull(FieldErrors.amountRes("512.40"))
        assertNull(FieldErrors.amountRes("2 750,25"))

        assertEquals(R.string.field_error_gallons_positive, FieldErrors.gallonsRes("0"))
        assertEquals(R.string.field_error_gallons_positive, FieldErrors.gallonsRes("-1"))
        assertNull(FieldErrors.gallonsRes("12.5"))

        assertNull(FieldErrors.milesRes(""))
        assertEquals(R.string.field_error_miles, FieldErrors.milesRes("nope"))
        assertEquals(R.string.field_error_miles, FieldErrors.milesRes("-3"))
        assertNull(FieldErrors.milesRes("850"))

        assertNull(FieldErrors.discountRes("", "4.00"))
        assertEquals(R.string.field_error_amount_example, FieldErrors.discountRes("abc", "4.00"))
        assertEquals(R.string.add_diesel_discount_above_price, FieldErrors.discountRes("5", "4.00"))
        assertNull(FieldErrors.discountRes("3.50", "4.00"))
    }
}
