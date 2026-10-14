package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice

class DieselPurchaseMathTest {

    @Test
    fun example_500gal_at_5_10_with_4_09_discount() {
        val gallons = 500.0
        val list = 5.10
        val discount = 4.09

        assertEquals(Money.fromDollars(2045.0), DieselPurchaseMath.paidTotal(gallons, list?.let(UnitPrice::fromDollars), discount?.let(UnitPrice::fromDollars))!!)
        assertEquals(Money.fromDollars(505.0), DieselPurchaseMath.savings(gallons, list?.let(UnitPrice::fromDollars), discount?.let(UnitPrice::fromDollars))!!)
    }

    @Test
    fun withoutDiscount_usesListPrice() {
        assertEquals(Money.fromDollars(2550.0), DieselPurchaseMath.paidTotal(500.0, UnitPrice.fromDollars(5.10), null)!!)
        assertNull(DieselPurchaseMath.savings(500.0, UnitPrice.fromDollars(5.10), null))
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
            totalAmount = Money.fromDollars(2045.0),
            gallons = 500.0,
            pricePerGallon = UnitPrice.fromDollars(5.10),
            discountPricePerGallon = UnitPrice.fromDollars(4.09),
            location = null,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = 1L,
        )
        assertEquals(Money.fromDollars(505.0), diesel.savingsAmount!!)
    }
}
