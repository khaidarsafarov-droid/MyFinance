package com.truckerload.data.remote

import android.content.Context
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.sync.TelegramBotForegroundService

/**
 * Validates a BotFather token, stores it, and starts the local Telegram service.
 */
object TelegramTokenActivator {

    suspend fun saveAndStart(context: Context, rawToken: String): Result<String> {
        val token = rawToken.trim()
        if (token.isBlank()) {
            return Result.failure(IllegalArgumentException("token_missing"))
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
}
