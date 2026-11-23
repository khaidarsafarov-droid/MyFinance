package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.local.DatabaseStartup
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапуск Telegram polling после перезагрузки телефона (только если есть активный аккаунт). */
class TelegramBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                // Journal open and session restore finish off this thread. Boot work
                // does not open Room on the main thread.
                if (!DatabaseStartup.awaitReadyBlockingOrFalse()) return@Thread
                LoadAlarmScheduler.rescheduleAll(appContext)
                val userId = AuthStore(appContext).currentUserIdOrNull() ?: return@Thread
                val tokenStore = TelegramTokenStore(appContext, userId)
                tokenStore.bootstrapFromBuildConfigIfEmpty()
                if (!tokenStore.hasToken()) return@Thread
                TelegramSyncWorker.enqueueEnsureService(appContext)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
