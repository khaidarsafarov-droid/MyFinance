package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Перезапускает foreground-сервис бота после убийства системой. */
class TelegramServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        if (TelegramBotForegroundService.canStart(app)) {
            TelegramBotForegroundService.start(app)
        }
    }
}
