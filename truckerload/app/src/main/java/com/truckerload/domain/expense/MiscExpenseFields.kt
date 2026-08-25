package com.truckerload.domain.expense

/** Validation and CSV helpers for miscellaneous expenses. */
object MiscExpenseFields {

    enum class Error {
        AMOUNT,
        DESCRIPTION,
        DATE,
    }

    fun parseAmount(raw: String): Double? {
        val cleaned = raw.trim()
            .replace('\u00A0', ' ')
            .replace(" ", "")
            .replace("$", "")
            .replace(',', '.')
        return cleaned.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    fun validate(amountText: String, description: String, dateIso: String): Error? {
        if (parseAmount(amountText) == null) return Error.AMOUNT
        if (description.trim().isEmpty()) return Error.DESCRIPTION
        if (!DATE_ISO.matches(dateIso.trim())) return Error.DATE
        return null
    }

    fun csvQuote(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private val DATE_ISO = Regex("""^\d{4}-\d{2}-\d{2}$""")
}
