package com.truckerload.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore

/** Перезапуск Telegram polling после перезагрузки телефона (только если есть активный аккаунт). */
class TelegramBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        LoadAlarmScheduler.rescheduleAll(appContext)
        if (TelegramSyncMode.isServer()) {
            ServerTelegramInboxWorker.enqueue(appContext)
            ServerTelegramInboxWorker.enqueuePeriodic(appContext)
            return
        }
        val userId = AuthStore(appContext).currentUserIdOrNull() ?: return
        val tokenStore = TelegramTokenStore(appContext, userId)
        tokenStore.bootstrapFromBuildConfigIfEmpty()
        if (!tokenStore.hasToken()) return
        TelegramSyncWorker.enqueueEnsureService(appContext)
    }
}
