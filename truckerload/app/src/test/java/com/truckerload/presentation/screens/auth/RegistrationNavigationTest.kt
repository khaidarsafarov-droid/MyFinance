package com.truckerload.presentation.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class RegistrationNavigationTest {

    @Test
    fun firstStepLeavesTheFlow() {
        assertEquals(RegistrationBackAction.Leave, RegistrationNavigation.onBack(0))
        assertEquals(RegistrationBackAction.Leave, RegistrationNavigation.onBack(-1))
        assertEquals(0, RegistrationNavigation.previousStep(0))
    }

    @Test
    fun laterStepsGoBackOne() {
        assertEquals(RegistrationBackAction.Previous, RegistrationNavigation.onBack(1))
        assertEquals(RegistrationBackAction.Previous, RegistrationNavigation.onBack(2))
        assertEquals(1, RegistrationNavigation.previousStep(2))
        assertEquals(0, RegistrationNavigation.previousStep(1))
    }
}
