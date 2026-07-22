package com.truckerload.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramAuthErrorsSmokeTest {
    @Test
    fun stopsOn401() {
        assertTrue(TelegramAuthErrors.shouldStopService("HTTP 401"))
    }

    @Test
    fun keepsGoingOnTimeout() {
        assertFalse(TelegramAuthErrors.shouldStopService("timeout"))
    }
}
