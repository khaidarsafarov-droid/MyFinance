package com.truckerload.domain.paycheck

import java.util.Locale

/** Net amount for paycheck edit / add forms. */
object PaycheckSalaryFields {

    enum class Error {
        NET,
    }

    fun parseAmount(raw: String): Double? =
        parseAmountAllowingZero(raw)?.takeIf { it > 0.0 }

    /**
     * Locale-aware money parse (US `2,500.50` and EU `2500,50` / `1.234,56`).
     * Returns 0 and negatives; [parseAmount] still requires a positive value.
     */
    fun parseAmountAllowingZero(raw: String): Double? {
        val trimmed = raw.trim()
            .replace('\u00A0', ' ')
            .replace(" ", "")
            .replace("$", "")
        if (trimmed.isEmpty()) return null
        val lastComma = trimmed.lastIndexOf(',')
        val lastDot = trimmed.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    trimmed.replace(".", "").replace(',', '.')
                } else {
                    trimmed.replace(",", "")
                }
            }
            lastComma >= 0 -> {
                val fraction = trimmed.length - lastComma - 1
                if (fraction == 2) trimmed.replace(',', '.') else trimmed.replace(",", "")
            }
            else -> trimmed
        }
        return normalized.toDoubleOrNull()
    }

    fun validate(netText: String): Error? =
        if (parseAmount(netText) == null) Error.NET else null

    fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else String.format(Locale.US, "%.2f", amount)
}
