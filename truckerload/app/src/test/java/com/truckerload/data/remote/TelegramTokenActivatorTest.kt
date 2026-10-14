package com.truckerload.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TelegramTokenActivatorTest {

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
}
