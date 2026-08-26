package com.truckerload.utils

/** Shared validation for Add Paycheck / Add Diesel amount fields. */
object AmountInputValidator {

    /**
     * Parses a positive money amount. Returns null when blank, non-numeric, zero, or negative.
     */
    fun parsePositiveAmount(raw: String): Double? {
        // FIX: Add Paycheck/Diesel rejected EU "2 750,25" while the edit dialog accepted it
        return com.truckerload.domain.paycheck.PaycheckSalaryFields.parseAmount(raw)
    }

    fun isValidPositiveAmount(raw: String): Boolean = parsePositiveAmount(raw) != null
}
