package com.truckerload.utils

/** Shared validation for Add Paycheck / Add Diesel amount fields. */
object AmountInputValidator {

    /**
     * Parses a positive money amount. Returns null when blank, non-numeric, zero, or negative.
     */
    fun parsePositiveAmount(raw: String): Double? {
        val value = raw.trim().toDoubleOrNull() ?: return null
        return if (value > 0.0) value else null
    }

    fun isValidPositiveAmount(raw: String): Boolean = parsePositiveAmount(raw) != null
}
