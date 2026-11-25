package com.truckerload.presentation.screens.home

import android.app.Application
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramBotHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether the Telegram bot token is set and Telegram accepts it. Drives the Home status dot. */
internal class HomeBotStatus(private val app: Application) {

    enum class State { NOT_CONFIGURED, ACTIVE, INACTIVE }

    suspend fun check(): State {
        val store = TelegramTokenStore(app)
        if (!store.hasToken()) return State.NOT_CONFIGURED
        val token = store.getToken()
        val health = withContext(Dispatchers.IO) { TelegramBotHealth.check(token) }
        return if (health.ok) State.ACTIVE else State.INACTIVE
    }
}
