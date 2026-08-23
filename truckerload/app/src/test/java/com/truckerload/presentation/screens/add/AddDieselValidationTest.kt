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

    @Test
    fun uiState_computesPaidTotalAndSavings() {
        val state = AddDieselUiState(
            gallonsText = "500",
            pricePerGallonText = "5.10",
            discountPriceText = "4.09",
        )
        assertEquals(500.0, state.gallons!!, 0.0)
        assertEquals(5.10, state.pricePerGallon!!, 0.0)
        assertEquals(4.09, state.discountPricePerGallon!!, 0.0)
        assertEquals(2045.0, state.paidTotal!!, 0.001)
        assertEquals(505.0, state.savings!!, 0.001)
    }
}
