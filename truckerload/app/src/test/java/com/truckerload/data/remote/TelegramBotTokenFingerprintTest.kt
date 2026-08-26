package com.truckerload.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramBotTokenFingerprintTest {

    @Test
    fun of_isStableAndDoesNotEmbedToken() {
        val token = "123456789:AA" + "x".repeat(35)
        val first = TelegramBotTokenFingerprint.of(token)
        val second = TelegramBotTokenFingerprint.of(token)
        assertEquals(first, second)
        assertEquals(16, first.length)
        assertTrue(first.matches(Regex("[0-9a-f]{16}")))
        assertTrue(!first.contains("123456789"))
        assertTrue(!first.contains("xxx"))
    }

    @Test
    fun of_changesWhenTokenChanges() {
        val a = TelegramBotTokenFingerprint.of("123456789:AA" + "x".repeat(35))
        val b = TelegramBotTokenFingerprint.of("123456789:AA" + "y".repeat(35))
        assertNotEquals(a, b)
    }
}
