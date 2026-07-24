package com.truckerload.presentation.screens.add

import com.truckerload.utils.AmountInputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddDieselValidationTest {

    @Test
    fun dieselAmount_rejectsNonPositiveValues() {
        assertNull(AmountInputValidator.parsePositiveAmount("0"))
        assertNull(AmountInputValidator.parsePositiveAmount("-0.01"))
        assertNull(AmountInputValidator.parsePositiveAmount("-25"))
    }

    @Test
    fun dieselAmount_acceptsTrimmedPositiveValue() {
        assertEquals(125.75, AmountInputValidator.parsePositiveAmount(" 125.75 ")!!, 0.0)
    }
}
