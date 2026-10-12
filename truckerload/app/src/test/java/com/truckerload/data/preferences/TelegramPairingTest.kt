package com.truckerload.data.preferences

import com.truckerload.sync.telegram.telegramStartPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramPairingTest {

    @Test
    fun pairingCodeIsStableUntilConsumedAndSingleUse() = runBlocking {
        val store = SettingsDataStore(RuntimeEnvironment.getApplication())
        val code = store.getOrCreateTelegramPairingCode()
        assertEquals(8, code.length)
        assertEquals(code, store.getOrCreateTelegramPairingCode())

        assertFalse(store.consumeTelegramPairingCode("WRONG123"))
        assertFalse(store.consumeTelegramPairingCode(""))
        assertTrue(store.consumeTelegramPairingCode(code.lowercase()))
        assertFalse(store.consumeTelegramPairingCode(code))
        assertNotEquals(code, store.getOrCreateTelegramPairingCode())
    }

    @Test
    fun unpairClearsChatAndRotatesCode() = runBlocking {
        val store = SettingsDataStore(RuntimeEnvironment.getApplication())
        store.saveTelegramChatId(42L)
        assertEquals(42L, store.getTelegramChatIdOnce())
        assertEquals(42L, store.observeTelegramChatId().first())
        val before = store.getOrCreateTelegramPairingCode()

        store.clearTelegramChatId()

        assertNull(store.getTelegramChatIdOnce())
        assertNull(store.observeTelegramChatId().first())
        assertNotEquals(before, store.getOrCreateTelegramPairingCode())
    }

    @Test
    fun startPayloadNeedsTheStartCommandWithAnArgument() {
        assertEquals("ABC23XYZ", telegramStartPayload("/start ABC23XYZ"))
        assertEquals("ABC23XYZ", telegramStartPayload("  /start@my_bot  ABC23XYZ "))
        assertNull(telegramStartPayload("/start"))
        assertNull(telegramStartPayload("/starter ABC"))
        assertNull(telegramStartPayload("hello /start ABC"))
    }
}
