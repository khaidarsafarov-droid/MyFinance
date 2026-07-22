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
    fun redactsBearerToken() {
        val raw = "Authorization: Bearer supersecrettokenvalue123"
        val redacted = LogRedactor.redact(raw)
        assertFalse(redacted.contains("supersecrettokenvalue123"))
        assertTrue(redacted.contains("Bearer ***"))
    }
}
