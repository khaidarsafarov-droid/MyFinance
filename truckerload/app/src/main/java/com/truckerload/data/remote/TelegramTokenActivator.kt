package com.truckerload.data.remote

import android.content.Context
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.sync.TelegramBotForegroundService

/**
 * Validates a BotFather token, stores it, and starts the local Telegram service.
 */
object TelegramTokenActivator {

    /**
     * Validates a BotFather token, stores it encrypted, and starts the local poller.
     *
     * @return bot username on success, or `token_missing` / `token_invalid` / network error.
     */
    suspend fun saveAndStart(context: Context, rawToken: String): Result<String> {
        val token = rawToken.trim()
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("token_missing"))
        }
        // FIX: reject obviously malformed tokens before calling Telegram (avoids leaking junk in logs)
        if (!isPlausibleToken(token)) {
            return Result.failure(IllegalStateException("token_invalid"))
        }
        val health = TelegramBotHealth.check(token)
        if (health.isUnauthorized) {
            return Result.failure(IllegalStateException("token_invalid"))
        }
        if (!health.ok) {
            return Result.failure(IllegalStateException(health.error ?: "token_check_failed"))
        }
        return runCatching {
            TelegramTokenStore(context).setToken(token)
            TelegramBotForegroundService.stop(context)
            TelegramBotForegroundService.start(context)
            health.username.orEmpty()
        }
    }

    /**
     * BotFather tokens look like `123456789:AAH...` — digits, colon, then a long secret.
     */
    fun isPlausibleToken(token: String): Boolean = TOKEN_SHAPE.matches(token.trim())

    private val TOKEN_SHAPE = Regex("""^\d{5,16}:[A-Za-z0-9_-]{20,}$""")
}
