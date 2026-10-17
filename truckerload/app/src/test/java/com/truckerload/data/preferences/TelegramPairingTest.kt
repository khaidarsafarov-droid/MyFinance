package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.sync.telegram.TelegramPairHint
import com.truckerload.sync.telegram.isStartCommand
import com.truckerload.sync.telegram.shouldPairOnStart
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

    @Test
    fun startButtonIsAStartCommandAndPairsAPrivateChat() {
        assertTrue(isStartCommand("/start"))
        assertTrue(isStartCommand(" /START "))
        assertTrue(isStartCommand("/start@TruckoRigBot"))
        assertTrue(isStartCommand("/start ABC"))
        assertFalse(isStartCommand("/starter"))
        assertFalse(isStartCommand("hello"))

        assertTrue(shouldPairOnStart("/start", isCallbackQuery = false, chatType = "private"))
        assertTrue(shouldPairOnStart("/start", isCallbackQuery = false, chatType = ""))
        assertFalse(shouldPairOnStart("/start", isCallbackQuery = true, chatType = "private"))
        assertFalse(shouldPairOnStart("/start", isCallbackQuery = false, chatType = "group"))
        assertFalse(shouldPairOnStart("Trip ID: T-1", isCallbackQuery = false, chatType = "private"))
    }

    @Test
    fun pairHintIsSentOncePerChat() {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("pair-hint-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertTrue(TelegramPairHint.markIfFirst(prefs, "42"))
        assertFalse(TelegramPairHint.markIfFirst(prefs, "42"))
        assertTrue(TelegramPairHint.markIfFirst(prefs, "99"))
        assertFalse(TelegramPairHint.markIfFirst(prefs, ""))
    }
}
