package com.truckerload.presentation.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat
import java.util.Locale

class MoneyFormatTest {
    @Test
    fun currencySymbol_comesFromUsCurrencyFormatter() {
        val expected = NumberFormat.getCurrencyInstance(Locale.US).currency?.symbol ?: "$"
        assertEquals(expected, MoneyFormat.currencySymbol)
    }
}
