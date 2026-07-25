package com.truckerload.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramSyncModeTest {
    @Test
    fun `server is explicit and all invalid values preserve device mode`() {
        assertEquals(TelegramSyncMode.SERVER, TelegramSyncMode.resolve(" SERVER "))
        assertEquals(TelegramSyncMode.DEVICE, TelegramSyncMode.resolve("device"))
        assertEquals(TelegramSyncMode.DEVICE, TelegramSyncMode.resolve("invalid"))
        assertEquals(TelegramSyncMode.DEVICE, TelegramSyncMode.resolve(null))
    }

    @Test
    fun `server inbox deterministically ignores commands but processes ordinary text`() {
        assertTrue(ServerTelegramMessageProcessor.shouldIgnore("/status"))
        assertTrue(ServerTelegramMessageProcessor.shouldIgnore("  /start token"))
        assertFalse(ServerTelegramMessageProcessor.shouldIgnore("Trip ID: T-123"))
    }
}
