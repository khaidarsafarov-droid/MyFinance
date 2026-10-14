package com.truckerload.shared

import com.truckerload.contract.PushPlatforms
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedBusinessLogicTest {
    @Test
    fun `dailyTarget matches domain math used by Android`() {
        assertEquals(
            10_000L,
            SharedBusinessLogic.dailyTargetCents(goalCents = 70_000L, totalGrossCents = 20_000L, daysRemaining = 5),
        )
    }

    @Test
    fun `expectedGrossByNow is linear across a seven day week`() {
        assertEquals(10_000L, SharedBusinessLogic.expectedGrossByNowCents(goalCents = 70_000L, daysActive = 1))
        assertEquals(0L, SharedBusinessLogic.expectedGrossByNowCents(goalCents = 70_000L, daysActive = 0))
    }

    @Test
    fun `iOS push platform label stays stable`() {
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
