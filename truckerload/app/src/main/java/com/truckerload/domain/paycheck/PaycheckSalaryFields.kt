package com.truckerload.domain.paycheck

import java.util.Locale

/** Net (required) and optional gross for paycheck edit / add forms. */
object PaycheckSalaryFields {

    enum class Error {
        NET,
        GROSS,
    }

    fun parseAmount(raw: String): Double? {
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
        return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    fun parseOptionalAmount(raw: String): Double? {
        if (raw.trim().isEmpty()) return null
        return parseAmount(raw)
    }

    fun validate(netText: String, grossText: String): Error? {
        if (parseAmount(netText) == null) return Error.NET
        if (grossText.trim().isNotEmpty() && parseAmount(grossText) == null) return Error.GROSS
        return null
    }

    fun formatAmount(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else String.format(Locale.US, "%.2f", amount)
}
