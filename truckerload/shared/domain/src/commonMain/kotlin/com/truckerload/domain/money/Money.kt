package com.truckerload.domain.money

import kotlin.math.abs

/**
 * Currency amount stored as whole cents. Arithmetic stays on [Long] so
 * 0.10 added many times does not drift.
 */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun times(quantity: Int): Money = Money(cents * quantity.toLong())

    operator fun times(quantity: Long): Money = Money(cents * quantity)

    /** Whole-unit quantity, such as a count of identical amounts. */
    fun timesQuantity(quantity: Long): Money = this * quantity

    /** Scales by a factor such as 1.05 and rounds half-up to the nearest cent. */
    fun scaledBy(factor: Double): Money {
        if (factor == 0.0 || cents == 0L) return ZERO
        val scaled = kotlin.math.round(cents * factor).toLong()
        return Money(scaled)
    }

    fun dividedBy(divisor: Int): Money {
        if (divisor == 0) return ZERO
        val negative = (cents < 0) != (divisor < 0)
        val dividend = abs(cents)
        val divider = abs(divisor.toLong())
        val rounded = (dividend + divider / 2) / divider
        return Money(if (negative) -rounded else rounded)
    }

    fun coerceAtLeast(minimum: Money): Money = if (this < minimum) minimum else this

    fun isZero(): Boolean = cents == 0L

    fun isPositive(): Boolean = cents > 0L

    /** Plain decimal dollars. String templates must not print `Money(cents=…)`. */
    override fun toString(): String = formatPlain()

    /** US dollar text with exactly two decimals, no thousands separator. */
    fun formatPlain(): String {
        val negative = cents < 0
        val absCents = abs(cents)
        val dollars = absCents / 100
        val fraction = (absCents % 100).toString().padStart(2, '0')
        return buildString {
            if (negative) append('-')
            append(dollars)
            append('.')
            append(fraction)
        }
    }

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO = Money(0L)

        fun fromCents(cents: Long): Money = Money(cents)

        /** Parses a decimal dollar string such as "12.34" or "-0.10". */
        fun parse(text: String): Money {
            val trimmed = text.trim().removePrefix("$").replace(",", "")
            require(trimmed.isNotEmpty()) { "empty money" }
            val negative = trimmed.startsWith("-")
            val body = trimmed.removePrefix("-").removePrefix("+")
            val parts = body.split('.')
            require(parts.size <= 2) { "invalid money: $text" }
            val dollars = parts[0].ifEmpty { "0" }.toLong()
            val fractionRaw = parts.getOrElse(1) { "0" }.padEnd(3, '0')
            var fraction = fractionRaw.take(2).toLong()
            if (fractionRaw.getOrNull(2)?.digitToIntOrNull()?.let { it >= 5 } == true) {
                fraction += 1
            }
            var cents = dollars * 100 + fraction
            if (fraction == 100L) {
                cents = (dollars + 1) * 100
            }
            return Money(if (negative) -cents else cents)
        }

        /**
         * Converts a dollar amount that is already a decimal quantity.
         * Uses the shortest round-trip of [value] then half-up to cents.
         */
        fun fromDollars(value: Double): Money {
            if (value.isNaN() || value.isInfinite()) return ZERO
            val text = value.toString()
            val parsed = runCatching { parse(text) }.getOrElse {
                val scaled = kotlin.math.round(value * 100.0).toLong()
                return Money(scaled)
            }
            return parsed
        }
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO, Money::plus)
