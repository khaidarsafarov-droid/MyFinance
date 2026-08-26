package com.truckerload.domain.paycheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaycheckSalaryFieldsTest {

    @Test
    fun parseAmount_acceptsCommaDollarAndSpaces() {
        assertEquals(10907.79, PaycheckSalaryFields.parseAmount("$10,907.79")!!, 0.001)
        assertEquals(2750.25, PaycheckSalaryFields.parseAmount("2 750,25")!!, 0.001)
        assertNull(PaycheckSalaryFields.parseAmount("0"))
        assertNull(PaycheckSalaryFields.parseAmount("abc"))
    }

    @Test
    fun parseOptionalAmount_blankIsNull() {
        assertNull(PaycheckSalaryFields.parseOptionalAmount(""))
        assertNull(PaycheckSalaryFields.parseOptionalAmount("  "))
        assertEquals(3200.0, PaycheckSalaryFields.parseOptionalAmount("3200")!!, 0.001)
    }

    @Test
    fun validate_requiresPositiveNet() {
        assertEquals(PaycheckSalaryFields.Error.NET, PaycheckSalaryFields.validate("", ""))
        assertEquals(PaycheckSalaryFields.Error.NET, PaycheckSalaryFields.validate("0", "100"))
        assertNull(PaycheckSalaryFields.validate("2500.50", ""))
        assertNull(PaycheckSalaryFields.validate("2500.50", "3000"))
        assertEquals(PaycheckSalaryFields.Error.GROSS, PaycheckSalaryFields.validate("2500", "abc"))
    }

    @Test
    fun formatAmount_stripsTrailingZeros() {
        assertEquals("2750", PaycheckSalaryFields.formatAmount(2750.0))
        assertEquals("2750.25", PaycheckSalaryFields.formatAmount(2750.25))
    }
}
