package com.truckerload.presentation.screens.goal

import com.truckerload.domain.parser.DecimalInput

/**
 * Pure input rules for the weekly profit goal editor.
 */
object WeeklyGoalInputValidator {

    fun sanitize(value: String): String =
        value.filter { c -> c.isDigit() || c == '.' || c == ',' }

    fun parseGoalAmount(value: String): Double? = DecimalInput.parse(value)
}
