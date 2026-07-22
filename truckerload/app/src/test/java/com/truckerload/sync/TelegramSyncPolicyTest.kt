package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramSyncPolicyTest {
    @Test
    fun backoffGrowsAndCaps() {
        assertEquals(1_000L, TelegramSyncPolicy.backoffDelayMs(0))
        assertEquals(2_000L, TelegramSyncPolicy.backoffDelayMs(1))
        assertEquals(30_000L, TelegramSyncPolicy.backoffDelayMs(10))
    }

    @Test
    fun retryableNetworkButNotUnauthorized() {
        assertTrue(TelegramSyncPolicy.isRetryable("timeout"))
        assertTrue(TelegramSyncPolicy.isRetryable("connection reset"))
        assertFalse(TelegramSyncPolicy.isRetryable("401 unauthorized"))
        assertFalse(TelegramSyncPolicy.isRetryable("invalid token"))
    }

    @Test
    fun shouldResetOffsetOnlyOnConflict() {
        assertTrue(TelegramSyncPolicy.shouldResetOffset(conflictDetected = true, unauthorized = false))
        assertFalse(TelegramSyncPolicy.shouldResetOffset(conflictDetected = true, unauthorized = true))
        assertFalse(TelegramSyncPolicy.shouldResetOffset(conflictDetected = false, unauthorized = false))
    }
}
