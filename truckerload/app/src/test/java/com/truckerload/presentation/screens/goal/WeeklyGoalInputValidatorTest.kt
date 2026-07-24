package com.truckerload.presentation.screens.goal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeeklyGoalInputValidatorTest {

    @Test
    fun parseGoalAmount_rejectsBlank() {
        assertNull(WeeklyGoalInputValidator.parseGoalAmount(""))
        assertNull(WeeklyGoalInputValidator.parseGoalAmount("   "))
    }

    @Test
    fun parseGoalAmount_acceptsWholeNumber() {
        assertEquals(2500.0, WeeklyGoalInputValidator.parseGoalAmount("2500")!!, 0.0)
    }

    @Test
    fun parseGoalAmount_acceptsCommaDecimalSeparator() {
        assertEquals(1250.75, WeeklyGoalInputValidator.parseGoalAmount("1250,75")!!, 0.0)
    }

    @Test
    fun parseGoalAmount_rejectsNonNumericInput() {
        assertNull(WeeklyGoalInputValidator.parseGoalAmount("abc"))
    }

    @Test
    fun sanitize_keepsDigitsAndDecimalSeparatorsOnly() {
        assertEquals("123.45,6", WeeklyGoalInputValidator.sanitize("${'$'}123.45,6abc"))
    }
}
