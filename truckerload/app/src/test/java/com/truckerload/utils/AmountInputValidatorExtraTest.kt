package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputValidatorExtraTest {
    @Test fun rejectsZero() = assertNull(AmountInputValidator.parsePositiveAmount("0"))
    @Test fun rejectsNegative() = assertNull(AmountInputValidator.parsePositiveAmount("-1"))
    @Test fun acceptsPositive() = assertEquals(12.5, AmountInputValidator.parsePositiveAmount("12.5")!!, 0.0)
}
