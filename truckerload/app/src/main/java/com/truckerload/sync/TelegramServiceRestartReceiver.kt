package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапускает foreground-сервис бота после убийства системой. */
class TelegramServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val userId = AuthStore(app).currentUserIdOrNull() ?: return
        if (TelegramTokenStore(app, userId).hasToken()) {
            TelegramBotForegroundService.start(app)
        }
    }
}
