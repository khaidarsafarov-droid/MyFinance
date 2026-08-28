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
    fun `server inbox shouldIgnore only blank text`() {
        assertFalse(ServerTelegramMessageProcessor.shouldIgnore("/status"))
        assertFalse(ServerTelegramMessageProcessor.shouldIgnore("  /start token"))
        assertTrue(ServerTelegramMessageProcessor.shouldIgnore("   "))
        assertFalse(ServerTelegramMessageProcessor.shouldIgnore("Trip ID: T-123"))
    }
}
