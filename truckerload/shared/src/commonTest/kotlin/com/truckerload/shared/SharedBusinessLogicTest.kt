package com.truckerload.shared

import com.truckerload.contract.PushPlatforms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedBusinessLogicTest {
    @Test
    fun `dailyTarget matches domain math used by Android`() {
        assertEquals(
            100.0,
            SharedBusinessLogic.dailyTarget(goal = 700.0, totalGross = 200.0, daysRemaining = 5),
        )
    }

    @Test
    fun `expectedGrossByNow is linear across a seven day week`() {
        assertEquals(100.0, SharedBusinessLogic.expectedGrossByNow(goal = 700.0, daysActive = 1))
        assertEquals(0.0, SharedBusinessLogic.expectedGrossByNow(goal = 700.0, daysActive = 0))
    }

    @Test
    fun `iOS push platform is the contract token the backend already stores`() {
        assertEquals(PushPlatforms.IOS, SharedBusinessLogic.iosPushPlatform())
    }

    @Test
    fun `Apple remains a reserved auth provider name`() {
        assertTrue("APPLE" in SharedBusinessLogic.reservedAuthProviders())
    }

    @Test
    fun `epochMillis is a positive wall clock`() {
        assertTrue(SharedBusinessLogic.epochMillis() > 0L)
    }
}
