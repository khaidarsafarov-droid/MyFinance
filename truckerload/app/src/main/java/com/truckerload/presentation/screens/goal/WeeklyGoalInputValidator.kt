package com.truckerload.presentation.screens.goal

/**
 * Pure input rules for the weekly profit goal editor.
 */
object WeeklyGoalInputValidator {

    fun sanitize(value: String): String =
        value.filter { c -> c.isDigit() || c == '.' || c == ',' }

    /** Parses a positive goal amount; blank, non-numeric, and ≤ 0 are rejected. */
    fun parseGoalAmount(value: String): Double? =
        value.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0.0 }
}
