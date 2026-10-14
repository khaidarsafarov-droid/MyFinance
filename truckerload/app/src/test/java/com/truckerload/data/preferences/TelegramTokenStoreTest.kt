package com.truckerload.data.preferences

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramTokenStoreTest {

    @Before
    fun setUp() {
        SecurePreferences.resetFallbackForTests()
    }

    @After
    fun tearDown() {
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun saveAndReadRoundTrip() {
        val context = RuntimeEnvironment.getApplication()
        val token = "123456789:AA" + "s".repeat(35)
        val store = TelegramTokenStore(context, "user-save")
        assertFalse(store.hasToken())
        store.setToken("  $token  ")
        assertEquals(token, store.getToken())
        assertTrue(store.hasPersistedToken())
        assertEquals(token, TelegramTokenStore(context, "user-save").getToken())
        store.clearToken()
        assertEquals("", TelegramTokenStore(context, "user-save").getToken())
        assertFalse(store.hasPersistedToken())
    }

    @Test
    fun migratesPlaintextLegacyToken() {
        val context = RuntimeEnvironment.getApplication()
        val token = "555555:PLAINTEXT" + "p".repeat(20)
        context.getSharedPreferences("telegram_token", Context.MODE_PRIVATE)
            .edit()
            .putString("bot_token", token)
            .commit()

        val store = TelegramTokenStore(context, "user-plain")
        assertEquals(token, store.getToken())
        assertEquals(
            "",
            context.getSharedPreferences("telegram_token", Context.MODE_PRIVATE)
                .getString("bot_token", null)
                .orEmpty(),
        )
        assertEquals(token, TelegramTokenStore(context, "user-plain").getToken())
    }

    @Test
    fun migratesEncryptedPrefsTokenAndDoesNotResurrectAfterClear() {
        val context = RuntimeEnvironment.getApplication()
        val userId = "user-enc"
        val token = "777777:ENC" + "e".repeat(24)
        val prefsName = "telegram_token_enc_" + AccountIds.sanitizeFilePart(userId)
        SecurePreferences.open(context, prefsName).edit()
            .putString("bot_token", token)
            .commit()

        val store = TelegramTokenStore(context, userId)
        assertEquals(token, store.getToken())
        store.clearToken()
        assertEquals("", TelegramTokenStore(context, userId).getToken())
        assertFalse(TelegramTokenStore(context, userId).hasPersistedToken())
    }
}
