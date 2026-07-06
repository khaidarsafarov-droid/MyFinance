package com.truckerload.sync

import androidx.core.content.edit
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Держит long-polling Telegram активным, пока приложение установлено.
 * Работает в фоне даже когда приложение закрыто (stopWithTask=false).
 */
class TelegramBotForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = TelegramTokenStore(applicationContext).getToken()
        if (token.isBlank()) {
            Log.w(TAG, "No TELEGRAM_BOT_TOKEN — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCompat()
        setupBotFeaturesOnce(token)
        if (pollJob?.isActive != true) {
            TelegramPollCoordinator.markForegroundPolling(true)
            pollJob = scope.launch {
                try {
                    pollLoop(token)
                } finally {
                    TelegramPollCoordinator.markForegroundPolling(false)
                }
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.w(TAG, "Task removed — scheduling bot service restart")
        TelegramServiceRestarter.schedule(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.w(TAG, "Service destroyed — scheduling restart")
        pollJob?.cancel()
        scope.cancel()
        TelegramServiceRestarter.schedule(applicationContext)
        super.onDestroy()
    }

    private fun setupBotFeaturesOnce(token: String) {
        val prefs = getSharedPreferences(TelegramSyncWorker.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BOT_FEATURES_SETUP, false)) return
        scope.launch {
            val api = TelegramApi(token)
            api.deleteWebhook().onFailure { e -> Log.w(TAG, "deleteWebhook: ${e.message}") }
            api.setMyCommands().onSuccess {
                prefs.edit {putBoolean(KEY_BOT_FEATURES_SETUP, true)}
            }.onFailure { e -> Log.w(TAG, "setMyCommands: ${e.message}") }
            api.setChatMenuButton().onFailure { e -> Log.w(TAG, "setChatMenuButton: ${e.message}") }
        }
    }

    private suspend fun pollLoop(token: String) {
        val engine = TelegramBotSyncEngine(applicationContext)
        while (scope.isActive) {
            try {
                val result = engine.runOnce(token)
                val delaySec = when {
                    result.processedUpdates > 0 -> result.nextDelaySeconds.coerceIn(1, 60)
                    else -> result.nextDelaySeconds.coerceIn(2, 60)
                }
                delay(delaySec * 1000)
            } catch (e: Exception) {
                Log.e(TAG, "pollLoop error — retrying", e)
                delay(5_000)
            }
        }
    }

    private fun startForegroundCompat() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.telegram_bot_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.telegram_bot_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle(getString(R.string.telegram_bot_notification_title))
            .setContentText(getString(R.string.telegram_bot_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val TAG = "TelegramBotFgService"
        private const val CHANNEL_ID = "telegram_bot_sync"
        private const val NOTIFICATION_ID = 4101
        private const val KEY_BOT_FEATURES_SETUP = "bot_features_setup_v3"

        fun start(context: Context) {
            if (TelegramTokenStore(context).getToken().isBlank()) return
            val intent = Intent(context, TelegramBotForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start foreground bot service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TelegramBotForegroundService::class.java))
        }
    }
}
