package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {
    @Test
    fun redactsTelegramBotTokenInUrl() {
        val raw = "https://api.telegram.org/bot123456:AAHsecrettokenvalue/getMe failed"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("AAHsecrettokenvalue"))
        assertTrue(redacted.contains("/bot***"))
    }

    @Test
    fun redactsJwt() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature"
        assertEquals("***jwt***", LogRedactor.redact(jwt))
    }

    @Test
    fun redactedThrowableDropsTheBotTokenAndTheCause() {
        val token = "123456789:AAHsecretTokenValue_xx"
        val root = IllegalStateException("https://api.telegram.org/bot$token/getUpdates")
        val wrapped = RuntimeException("call failed", root)
        val safe = LogRedactor.redactThrowable(wrapped)
        val text = safe.toString() + (safe.cause?.toString() ?: "")
        assertFalse(text.contains(token))
        assertFalse(text.contains("AAHsecretTokenValue"))
        assertTrue(text.contains("/bot***"))
        assertEquals(null, safe.cause)
    }

    @Test
    fun redactsGoogleApiKey() {
        val raw = "x-goog-api-key: AIzaSyA1234567890abcdefg"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("AIzaSyA1234567890abcdefg"))
        assertTrue(redacted.contains("***gkey***"))
    }
}
