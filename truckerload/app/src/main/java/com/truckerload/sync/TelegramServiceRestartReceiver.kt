package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапускает foreground-сервис бота после убийства системой. */
class TelegramServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (TelegramTokenStore(context.applicationContext).hasToken()) {
            TelegramBotForegroundService.start(context.applicationContext)
        }
    }
}
