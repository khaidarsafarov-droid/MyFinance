package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DieselPurchaseMathTest {

    @Test
    fun example_500gal_at_5_10_with_4_09_discount() {
        val gallons = 500.0
        val list = 5.10
        val discount = 4.09

        assertEquals(2045.0, DieselPurchaseMath.paidTotal(gallons, list, discount)!!, 0.001)
        assertEquals(505.0, DieselPurchaseMath.savings(gallons, list, discount)!!, 0.001)
    }

    @Test
    fun withoutDiscount_usesListPrice() {
        assertEquals(2550.0, DieselPurchaseMath.paidTotal(500.0, 5.10, null)!!, 0.001)
        assertNull(DieselPurchaseMath.savings(500.0, 5.10, null))
    }

    @Test
    fun diesel_savingsAmount_matchesMath() {
        val diesel = Diesel(
            id = 1,
            weekNumber = 34,
            year = 2026,
            weekLabel = "W34",
            weekStartDate = "2026-08-16",
            weekEndDate = "2026-08-22",
            totalAmount = 2045.0,
            gallons = 500.0,
            pricePerGallon = 5.10,
            discountPricePerGallon = 4.09,
            location = null,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = 1L,
        )
        assertEquals(505.0, diesel.savingsAmount!!, 0.001)
    }
}
