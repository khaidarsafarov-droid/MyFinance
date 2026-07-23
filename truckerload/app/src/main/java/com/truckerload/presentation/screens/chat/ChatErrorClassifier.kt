package com.truckerload.presentation.screens.chat

import java.util.Locale

/**
 * Classifies low-level AI/chat failures into stable UI error categories.
 */
object ChatErrorClassifier {

    enum class Kind {
        API_KEY,
        RATE_LIMIT,
        UNAVAILABLE,
        TIMEOUT,
        NETWORK,
        EMPTY,
        GENERIC,
    }

    fun classify(error: Throwable, noResponseKeyword: String = ""): Kind {
        val message = error.message.orEmpty().lowercase(Locale.ROOT)
        val noResponse = noResponseKeyword.lowercase(Locale.ROOT)
        return when {
            "401" in message || "403" in message || "api key" in message -> Kind.API_KEY
            "429" in message || "rate" in message || "quota" in message || "limit" in message -> Kind.RATE_LIMIT
            "500" in message || "502" in message || "503" in message || "504" in message -> Kind.UNAVAILABLE
            "timeout" in message || "timed out" in message -> Kind.TIMEOUT
            "unable to resolve host" in message || "network" in message || "failed to connect" in message -> Kind.NETWORK
            "empty" in message || (noResponse.isNotBlank() && noResponse in message) -> Kind.EMPTY
            else -> Kind.GENERIC
        }
    }
}
