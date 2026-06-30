package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапуск Telegram polling после перезагрузки телефона. */
class TelegramBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        TelegramTokenStore(appContext).syncFromBuildConfig()
        if (TelegramTokenStore(appContext).hasToken()) {
            TelegramBotForegroundService.start(appContext)
        }
    }
}
