package com.truckerload.presentation.screens.chat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ChatErrorClassifierTest {

    @Test
    fun classify_unauthorized_returnsApiKey() {
        assertEquals(
            ChatErrorClassifier.Kind.API_KEY,
            ChatErrorClassifier.classify(IllegalStateException("HTTP 401 invalid api key")),
        )
    }

    @Test
    fun classify_rateLimitSignals_returnsRateLimit() {
        assertEquals(
            ChatErrorClassifier.Kind.RATE_LIMIT,
            ChatErrorClassifier.classify(IllegalStateException("429 quota limit exceeded")),
        )
    }

    @Test
    fun classify_serverErrors_returnsUnavailable() {
        assertEquals(
            ChatErrorClassifier.Kind.UNAVAILABLE,
            ChatErrorClassifier.classify(IllegalStateException("HTTP 503")),
        )
    }

    @Test
    fun classify_timeoutSignals_returnsTimeout() {
        assertEquals(
            ChatErrorClassifier.Kind.TIMEOUT,
            ChatErrorClassifier.classify(IOException("request timed out")),
        )
    }

    @Test
    fun classify_networkSignals_returnsNetwork() {
        assertEquals(
            ChatErrorClassifier.Kind.NETWORK,
            ChatErrorClassifier.classify(IOException("Unable to resolve host api.example.test")),
        )
    }

    @Test
    fun classify_emptySignals_returnsEmpty() {
        assertEquals(
            ChatErrorClassifier.Kind.EMPTY,
            ChatErrorClassifier.classify(IllegalStateException("empty response")),
        )
    }

    @Test
    fun classify_customNoResponseKeyword_returnsEmpty() {
        assertEquals(
            ChatErrorClassifier.Kind.EMPTY,
            ChatErrorClassifier.classify(
                IllegalStateException("service did not return content"),
                noResponseKeyword = "did not return content",
            ),
        )
    }

    @Test
    fun classify_unknownMessage_returnsGeneric() {
        assertEquals(
            ChatErrorClassifier.Kind.GENERIC,
            ChatErrorClassifier.classify(IllegalStateException("something else")),
        )
    }
}
