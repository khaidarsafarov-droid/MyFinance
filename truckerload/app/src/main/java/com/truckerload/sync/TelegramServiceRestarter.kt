package com.truckerload.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore

/** Планирует перезапуск foreground-сервиса бота после убийства задачи или процесса. */
object TelegramServiceRestarter {

    private const val REQUEST_CODE = 4102
    private const val ACTION_RESTART = "com.truckorig.action.RESTART_TELEGRAM_BOT"

    fun schedule(context: Context, delayMs: Long = 3_000L) {
        // After Android 15 dataSync timeout, immediate restarts are forbidden until
        // the user opens the app — skip AlarmManager so we don't fight the platform.
        if (TelegramFgsQuota.isPaused()) return
        val userId = AuthStore(context).currentUserIdOrNull() ?: return
        if (!TelegramTokenStore(context, userId).hasToken()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TelegramServiceRestartReceiver::class.java).apply {
            action = ACTION_RESTART
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        val triggerAt = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(1_000L)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
        }
    }
}
