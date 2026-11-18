package com.truckerload.domain.paycheck

import java.util.Locale
import com.truckerload.domain.money.Money
import com.truckerload.domain.parser.DecimalInput

/** Net amount for paycheck edit / add forms. */
object PaycheckSalaryFields {

    enum class Error {
        NET,
    }

    fun parseAmount(raw: String): Double? =
        parseAmountAllowingZero(raw)?.takeIf { it > 0.0 }

    /** Returns 0 and negatives; [parseAmount] still requires a positive value. */
    fun parseAmountAllowingZero(raw: String): Double? = DecimalInput.parse(raw)

    fun validate(netText: String): Error? =
        if (parseAmount(netText) == null) Error.NET else null

    fun formatAmount(amount: Money): String {
        val plain = amount.formatPlain()
        return if (plain.endsWith(".00")) plain.dropLast(3) else plain
    }

    fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else String.format(Locale.US, "%.2f", amount)
}
