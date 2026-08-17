package com.truckerload.data.remote

import com.truckerload.data.preferences.SecurePreferences
import kotlinx.coroutines.runBlocking
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
class TelegramTokenActivatorTest {

    @Before
    fun setUp() {
        SecurePreferences.resetFallbackForTests()
    }

    @After
    fun tearDown() {
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun rejectsJunkAndAcceptsBotFatherShape() {
        assertFalse(TelegramTokenActivator.isPlausibleToken(""))
        assertFalse(TelegramTokenActivator.isPlausibleToken("not-a-token"))
        assertFalse(TelegramTokenActivator.isPlausibleToken("123:short"))
        assertFalse(TelegramTokenActivator.isPlausibleToken("abc:AAAAAAAAAAAAAAAAAAAA"))
        assertTrue(
            TelegramTokenActivator.isPlausibleToken("123456789:AA" + "x".repeat(35)),
        )
    }

    @Test
    fun saveAndStartFailsWithSecureStorageCodeWhenKeystoreFallback() = runBlocking {
        SecurePreferences.markFallbackForTests()
        val result = TelegramTokenActivator.saveAndStart(
            RuntimeEnvironment.getApplication(),
            "123456789:AA" + "x".repeat(35),
        )
        assertTrue(result.isFailure)
        assertEquals("token_secure_storage", result.exceptionOrNull()?.message)
    }
}
