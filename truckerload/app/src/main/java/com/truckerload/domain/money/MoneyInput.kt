package com.truckerload.domain.money

import com.truckerload.domain.parser.DecimalInput

/** Driver-typed money. Text fields use [Money.formatPlain], never [Money.toString] of a debug dump. */
object MoneyInput {
    fun format(amount: Money): String = amount.formatPlain()

    fun format(amount: Money?): String = amount?.let(::format).orEmpty()

    /**
     * Cents from the same separator rules as [DecimalInput].
     * `2111.72` stays 211172 cents; a [Double] hop is not used.
     */
    fun parse(raw: String?): Money? {
        val canonical = DecimalInput.canonical(raw) ?: return null
        return runCatching { Money.parse(canonical) }.getOrNull()
    }
}
