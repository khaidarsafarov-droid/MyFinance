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
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.LogRedactor
import java.util.concurrent.atomic.AtomicBoolean
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
 *
 * Shows a single quiet ongoing notification (Android requires one for FGS).
 * The notification is min-importance, silent, and never re-alerted on restarts.
 */
class TelegramBotForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (TelegramSyncMode.isServer()) {
            suppressRestart = true
            stopSelf()
            return START_NOT_STICKY
        }
        val userId = AuthStore(applicationContext).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user — stopping Telegram service")
            stopSelf()
            return START_NOT_STICKY
        }
        val token = TelegramTokenStore(applicationContext, userId).getToken()
        if (token.isBlank()) {
            Log.w(TAG, "No TELEGRAM_BOT_TOKEN — stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // Promote to foreground immediately (Android 8+ / 12+ time limits),
        // but keep the shade entry quiet and non-alerting.
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
        if (TelegramSyncMode.isServer()) {
            super.onTaskRemoved(rootIntent)
            return
        }
        Log.w(TAG, "Task removed — scheduling bot service restart")
        TelegramServiceRestarter.schedule(applicationContext)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning.set(false)
        startRequested.set(false)
        pollJob?.cancel()
        scope.cancel()
        if (!suppressRestart && !TelegramSyncMode.isServer()) {
            Log.w(TAG, "Service destroyed — scheduling restart")
            TelegramServiceRestarter.schedule(applicationContext)
        } else {
            Log.i(TAG, "Service destroyed — restart suppressed (logout)")
        }
        super.onDestroy()
    }

    private fun setupBotFeaturesOnce(token: String) {
        val prefs = getSharedPreferences(TelegramSyncWorker.PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_BOT_FEATURES_SETUP, false)) return
        scope.launch {
            val api = TelegramApi(token)
            api.deleteWebhook().onFailure { e -> Log.w(TAG, "deleteWebhook: ${LogRedactor.redact(e.message)}") }
            api.setMyCommands().onSuccess {
                prefs.edit { putBoolean(KEY_BOT_FEATURES_SETUP, true) }
            }.onFailure { e -> Log.w(TAG, "setMyCommands: ${LogRedactor.redact(e.message)}") }
            api.setChatMenuButton().onFailure { e -> Log.w(TAG, "setChatMenuButton: ${LogRedactor.redact(e.message)}") }
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
        val manager = getSystemService(NotificationManager::class.java) ?: return
        // Drop the old LOW channel once so users don't keep a noisy leftover entry.
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.telegram_bot_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = getString(R.string.telegram_bot_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.telegram_bot_notification_title))
            .setContentText(getString(R.string.telegram_bot_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
        return builder.build()
    }

    companion object {
        private const val TAG = "TelegramBotFgService"
        /** Quiet MIN channel; legacy LOW id is deleted on first create. */
        private const val CHANNEL_ID = "telegram_bot_sync_quiet"
        private const val LEGACY_CHANNEL_ID = "telegram_bot_sync"
        private const val NOTIFICATION_ID = 4101
        private const val KEY_BOT_FEATURES_SETUP = "bot_features_setup_v3"

        @Volatile
        private var suppressRestart = false

        private val isRunning = AtomicBoolean(false)
        private val startRequested = AtomicBoolean(false)

        fun start(context: Context) {
            if (TelegramSyncMode.isServer()) return
            // Already alive or start already in flight — avoid startForegroundService spam.
            if (isRunning.get() || TelegramPollCoordinator.isForegroundPolling()) return
            if (!startRequested.compareAndSet(false, true)) return
            val userId = AuthStore(context).currentUserIdOrNull()
            if (userId == null) {
                startRequested.set(false)
                return
            }
            if (TelegramTokenStore(context, userId).getToken().isBlank()) {
                startRequested.set(false)
                return
            }
            suppressRestart = false
            val intent = Intent(context, TelegramBotForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                startRequested.set(false)
                Log.w(TAG, "Cannot start foreground bot service: ${LogRedactor.redact(e.message)}")
            }
        }

        fun stop(context: Context) {
            suppressRestart = true
            startRequested.set(false)
            TelegramPollCoordinator.markForegroundPolling(false)
            isRunning.set(false)
            context.stopService(Intent(context, TelegramBotForegroundService::class.java))
        }

        /** Stop for logout: do not schedule AlarmManager restart. */
        fun stopForLogout(context: Context) = stop(context)
    }
}
