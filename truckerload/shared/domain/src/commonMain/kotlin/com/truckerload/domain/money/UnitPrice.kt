package com.truckerload.domain.money

import kotlin.math.abs
import kotlin.math.round

/**
 * A per-unit price with three decimal places, stored as thousandths of a dollar.
 * Used for diesel price per gallon. Never stored as [Double].
 */
@JvmInline
value class UnitPrice(val thousandths: Long) : Comparable<UnitPrice> {
    operator fun minus(other: UnitPrice): UnitPrice = UnitPrice(thousandths - other.thousandths)

    fun isPositive(): Boolean = thousandths > 0L

    /**
     * Gallons times this price, rounded half-up to cents.
     * Gallons are taken to the hundredth so the product stays integer:
     * cents = thousandths * gallonHundredths / 1000.
     */
    fun timesGallons(gallons: Double): Money {
        if (gallons <= 0.0 || thousandths == 0L) return Money.ZERO
        val gallonHundredths = round(gallons * 100.0).toLong()
        if (gallonHundredths <= 0L) return Money.ZERO
        val negative = thousandths < 0
        val product = abs(thousandths) * gallonHundredths
        val cents = (product + 500L) / 1000L
        return Money(if (negative) -cents else cents)
    }

    fun formatPlain(): String {
        val negative = thousandths < 0
        val absValue = abs(thousandths)
        val dollars = absValue / 1000
        val fraction = (absValue % 1000).toString().padStart(3, '0')
        return buildString {
            if (negative) append('-')
            append(dollars)
            append('.')
            append(fraction)
        }
    }

    override fun compareTo(other: UnitPrice): Int = thousandths.compareTo(other.thousandths)

    companion object {
        val ZERO = UnitPrice(0L)

        fun fromThousandths(thousandths: Long): UnitPrice = UnitPrice(thousandths)

        fun parse(text: String): UnitPrice {
            val trimmed = text.trim().removePrefix("$").replace(",", "")
            val negative = trimmed.startsWith("-")
            val body = trimmed.removePrefix("-").removePrefix("+")
            val parts = body.split('.')
            require(parts.size <= 2) { "invalid unit price: $text" }
            val dollars = parts[0].ifEmpty { "0" }.toLong()
            val fractionText = parts.getOrElse(1) { "0" }.padEnd(3, '0').take(3)
            val fraction = fractionText.toLong()
            val thousandths = dollars * 1000 + fraction
            return UnitPrice(if (negative) -thousandths else thousandths)
        }

        fun fromDollars(value: Double): UnitPrice {
            if (value.isNaN() || value.isInfinite()) return ZERO
            return runCatching { parse(value.toString()) }.getOrElse {
                UnitPrice(round(value * 1000.0).toLong())
            }
        }
    }
}
