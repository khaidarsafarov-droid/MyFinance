package com.truckerload.domain.money

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyTest {
    @Test
    fun toStringIsPlainDecimal() {
        assertEquals("2111.72", Money.fromCents(211172).toString())
        assertEquals("0.05", Money.fromCents(5).toString())
        assertEquals("2111.72", Money.fromCents(211172).formatPlain())
    }

    @Test
    fun summingManyDimesIsExact() {
        val dime = Money.parse("0.10")
        var sum = Money.ZERO
        repeat(1000) { sum += dime }
        assertEquals(Money.parse("100.00"), sum)
        assertEquals("100.00", sum.formatPlain())
    }

    @Test
    fun plusMinusAndQuantity() {
        val start = Money.parse("12.34")
        assertEquals(Money.parse("12.35"), start + Money.parse("0.01"))
        assertEquals(Money.parse("10.00"), start - Money.parse("2.34"))
        assertEquals(Money.parse("24.68"), start.timesQuantity(2))
    }

    @Test
    fun dividedByRoundsHalfUp() {
        assertEquals(Money.parse("0.33"), Money.parse("1.00").dividedBy(3))
        assertEquals(Money.parse("100.00"), Money.parse("700.00").dividedBy(7))
    }

    @Test
    fun unitPriceTimesGallonsUsesThousandths() {
        val price = UnitPrice.parse("5.907")
        assertEquals(5907L, price.thousandths)
        assertEquals(Money.parse("590.70"), price.timesGallons(100.0))
    }
}
