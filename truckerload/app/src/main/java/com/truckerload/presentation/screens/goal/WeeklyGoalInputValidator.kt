package com.truckerload.presentation.screens.goal

/**
 * Pure input rules for the weekly profit goal editor.
 */
object WeeklyGoalInputValidator {

    fun sanitize(value: String): String =
        value.filter { c -> c.isDigit() || c == '.' || c == ',' }

    /**
     * Parses a weekly goal amount.
     *
     * Accepts:
     * - whole numbers: `2500`
     * - US thousands: `2,500` / `2,500.75`
     * - decimal comma: `1250,75`
     * - EU thousands + decimal: `2.500,75`
     */
    fun parseGoalAmount(value: String): Double? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        // FIX: "2,500" must not become 2.5 via naive comma→dot replace
        val normalized = when {
            // US: 1,234 or 1,234.56
            US_THOUSANDS.matches(trimmed) -> trimmed.replace(",", "")
            // EU: 1.234,56
            EU_THOUSANDS.matches(trimmed) ->
                trimmed.replace(".", "").replace(",", ".")
            // Decimal comma only (no thousands): 1250,75
            trimmed.contains(',') && !trimmed.contains('.') ->
                trimmed.replace(',', '.')
            else -> trimmed
        }
        return normalized.toDoubleOrNull()
    }

    private val US_THOUSANDS = Regex("""^\d{1,3}(,\d{3})+(\.\d+)?$""")
    private val EU_THOUSANDS = Regex("""^\d{1,3}(\.\d{3})+(,\d+)?$""")
}
