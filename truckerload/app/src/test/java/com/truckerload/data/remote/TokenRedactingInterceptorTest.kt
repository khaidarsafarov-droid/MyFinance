package com.truckerload.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TokenRedactingInterceptorTest {
    private val token = "123456789:AAHsecretTokenValue_xx"

    @Test
    fun thrownExceptionNeverContainsTheBotToken() {
        val client = OkHttpClient.Builder()
            .installSecretRedaction(enableHttpLogging = false)
            .addInterceptor { chain ->
                throw IOException("failed ${chain.request().url}")
            }
            .build()
        val error = runCatching {
            client.newCall(
                Request.Builder().url("https://api.telegram.org/bot$token/getMe").build(),
            ).execute()
        }.exceptionOrNull()
        assertNotNull(error)
        val text = buildString {
            append(error)
            append(error?.cause)
            append(error?.message)
        }
        assertFalse(text.contains(token))
        assertFalse(text.contains("AAHsecretTokenValue"))
        assertTrue(text.contains("/bot***"))
        assertNull(error?.cause)
    }

    @Test
    fun nestedCauseIsRedactedToo() {
        val root = IOException("https://api.telegram.org/bot$token/getUpdates")
        val wrapped = IOException("wrapper", root)
        val safe = TokenRedactingInterceptor.redacted(wrapped)
        assertFalse(safe.message.orEmpty().contains(token))
        assertFalse(safe.toString().contains("AAHsecretTokenValue"))
        assertNull(safe.cause)
        assertTrue(safe.message.orEmpty().contains("/bot***"))
    }

    @Test
    fun loggedUrlsNeverContainTheBotToken() {
        val logs = mutableListOf<String>()
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ok":true}"""))
        server.start()
        try {
            val client = OkHttpClient.Builder()
                .installSecretRedaction(enableHttpLogging = true) { logs += it }
                .build()
            val url = server.url("/bot$token/getMe")
            client.newCall(Request.Builder().url(url).build()).execute().close()
        } finally {
            server.shutdown()
        }
        val joined = logs.joinToString("\n")
        assertTrue(joined.isNotBlank())
        assertFalse(joined.contains(token))
        assertFalse(joined.contains("AAHsecretTokenValue"))
        assertTrue(joined.contains("/bot***"))
    }

    @Test
    fun releaseClientDoesNotInstallHttpLogging() {
        val client = OkHttpClient.Builder()
            .installSecretRedaction(enableHttpLogging = false)
            .build()
        assertTrue(client.interceptors.any { it is TokenRedactingInterceptor })
        assertFalse(client.interceptors.any { it is HttpLoggingInterceptor })
        assertFalse(client.networkInterceptors.any { it is HttpLoggingInterceptor })
    }

    @Test
    fun apiFailureMessageNeverContainsTheBotToken() {
        val failure = TelegramApi.redactedApiFailure(
            "getMe failed: 401 https://api.telegram.org/bot$token/getMe",
        )
        assertFalse(failure.message.orEmpty().contains(token))
        assertFalse(failure.toString().contains("AAHsecretTokenValue"))
        assertEquals(null, failure.cause)
        try {
            TelegramApi.requireTelegramOk(
                "getMe",
                401,
                "unauthorized https://api.telegram.org/bot$token/getMe",
            )
            throw AssertionError("expected failure")
        } catch (error: Exception) {
            assertFalse(error.message.orEmpty().contains(token))
            assertFalse(error.toString().contains("AAHsecretTokenValue"))
        }
    }
}
