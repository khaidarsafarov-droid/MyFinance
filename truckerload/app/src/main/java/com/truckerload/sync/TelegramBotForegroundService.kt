package com.truckerload.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.truckerload.R
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotBranding
import com.truckerload.data.remote.TelegramBotTokenFingerprint
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.LogRedactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs Telegram long-polling only while the app is in the foreground.
 * Stopped automatically when the app goes to background; background sync
 * is handled by [TelegramSyncWorker] via WorkManager (every ~15 min).
 *
 * Android 15 (targetSdk 35) caps [dataSync] FGS at ~6h / 24h in the background.
 * [onTimeout] must [stopSelf] immediately or the system crashes the process.
 */
class TelegramBotForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunningFlag.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // After startForegroundService() or a START_STICKY restart, Android requires
        // startForeground() within a few seconds — even when we decide to stop.
        // Skipping it causes RemoteServiceException / "TruckoRig keeps stopping".
        startForegroundCompat()

        val userId = AuthStore(applicationContext).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user — stopping Telegram service")
            stopQuietly(restart = false)
            return START_NOT_STICKY
        }
        val token = TelegramTokenStore(applicationContext, userId).getToken()
        if (token.isBlank()) {
            Log.w(TAG, "No TELEGRAM_BOT_TOKEN — stopping service")
            stopQuietly(restart = false)
            return START_NOT_STICKY
        }

        // FIX: clear in-flight start gate so a missed onStartCommand cannot block forever
        startRequested.set(false)
        isRunningFlag.set(true)
        setupBotFeaturesOnce(token)
        // FIX: always restart poll with the current account token (account switch must not reuse old job)
        pollJob?.cancel()
        TelegramPollCoordinator.markForegroundPolling(true)
        pollJob = scope.launch {
            try {
                pollLoop(token, userId)
            } finally {
                TelegramPollCoordinator.markForegroundPolling(false)
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Android 15+: dataSync quota exhausted. Must stop within a few seconds or the
     * system throws RemoteServiceException ("keeps stopping").
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "dataSync FGS timeout (type=$fgsType) — stopping to avoid crash")
        TelegramFgsQuota.markTimedOut(applicationContext)
        stopQuietly(restart = false)
    }

    /** Stop after startForeground(); optionally suppress AlarmManager restart. */
    private fun stopQuietly(restart: Boolean) {
        startRequested.set(false)
        isRunningFlag.set(false)
        pollJob?.cancel()
        TelegramPollCoordinator.markForegroundPolling(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "Task removed — stopping FGS (background sync via WorkManager)")
        stopQuietly(restart = false)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunningFlag.set(false)
        startRequested.set(false)
        pollJob?.cancel()
        scope.cancel()
        Log.i(TAG, "Service destroyed — background sync handled by WorkManager")
        super.onDestroy()
    }

    private fun setupBotFeaturesOnce(token: String) {
        val prefs = getSharedPreferences(TelegramSyncWorker.PREFS_NAME, MODE_PRIVATE)
        val tokenFp = TelegramBotTokenFingerprint.of(token)
        val featuresDone = storedTokenFingerprint(prefs, KEY_BOT_FEATURES_SETUP) == tokenFp
        if (featuresDone &&
            TelegramBotBranding.isNameApplied(prefs, token) &&
            TelegramBotBranding.isPhotoApplied(prefs, token)
        ) return
        scope.launch {
            val api = TelegramApi(token)
            if (!featuresDone) {
                api.deleteWebhook().onFailure { e ->
                    Log.w(TAG, "deleteWebhook: ${LogRedactor.redact(e.message)}")
                }
                api.setMyCommands().onSuccess {
                    // FIX: key setup by token fingerprint so account B's bot is not skipped
                    prefs.edit { putString(KEY_BOT_FEATURES_SETUP, tokenFp) }
                }.onFailure { e -> Log.w(TAG, "setMyCommands: ${LogRedactor.redact(e.message)}") }
                api.setChatMenuButton().onFailure { e ->
                    Log.w(TAG, "setChatMenuButton: ${LogRedactor.redact(e.message)}")
                }
            }
            TelegramBotBranding.apply(applicationContext, token)
        }
    }

    /** Legacy builds stored a Boolean for features-setup; reading it as String crashes. */
    private fun storedTokenFingerprint(prefs: SharedPreferences, key: String): String =
        prefs.all[key] as? String ?: ""

    private suspend fun pollLoop(token: String, expectedUserId: String) {
        val engine = TelegramBotSyncEngine(applicationContext)
        while (scope.isActive) {
            try {
                // FIX: abort if session switched mid-poll so updates never land in another DB
                val activeUserId = AuthStore(applicationContext).currentUserIdOrNull()
                if (activeUserId.isNullOrBlank() || activeUserId != expectedUserId) {
                    Log.w(TAG, "Active user changed — stopping pollLoop")
                    break
                }
                val activeToken = TelegramTokenStore(applicationContext, activeUserId).getToken()
                if (activeToken.isBlank() || activeToken != token) {
                    Log.w(TAG, "Bot token changed — stopping pollLoop")
                    break
                }
                val result = engine.runOnce(token, expectedUserId = expectedUserId)
                val delaySec = if (result.processedUpdates > 0) {
                    result.nextDelaySeconds.coerceIn(0, 60)
                } else {
                    result.nextDelaySeconds.coerceIn(2, 60)
                }
                if (delaySec > 0L) delay(delaySec * 1000)
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
        private const val KEY_BOT_FEATURES_SETUP = "bot_features_setup_v4"

        private val isRunningFlag = AtomicBoolean(false)
        private val startRequested = AtomicBoolean(false)

        /** True while the foreground bot service is alive. */
        fun isRunning(): Boolean = isRunningFlag.get()

        /** True when the bot FGS is allowed to run (logged in + token configured). */
        fun canStart(context: Context): Boolean {
            val userId = AuthStore(context).currentUserIdOrNull() ?: return false
            return TelegramTokenStore(context, userId).getToken().isNotBlank()
        }

        fun start(context: Context) {
            if (!canStart(context)) return
            // Quota exhausted — only resume when the user opens the app (resets the timer).
            if (TelegramFgsQuota.isPaused(context) && !isAppInForeground()) {
                Log.i(TAG, "dataSync quota paused — defer start until app is foreground")
                return
            }
            if (isAppInForeground()) {
                TelegramFgsQuota.clearPause(context.applicationContext)
            }
            // Already alive or start already in flight — avoid startForegroundService spam.
            if (isRunningFlag.get() || TelegramPollCoordinator.isForegroundPolling()) return
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
            val intent = Intent(context, TelegramBotForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                startRequested.set(false)
                // Quota exhausted / background start denied — remember so watchdog stops retrying.
                if (isDataSyncQuotaException(e)) {
                    TelegramFgsQuota.markTimedOut(context.applicationContext)
                }
                Log.w(TAG, "Cannot start foreground bot service: ${LogRedactor.redact(e.message)}")
            }
        }

        fun stop(context: Context) {
            startRequested.set(false)
            TelegramPollCoordinator.markForegroundPolling(false)
            isRunningFlag.set(false)
            context.stopService(Intent(context, TelegramBotForegroundService::class.java))
        }

        /** Stop for logout: do not schedule AlarmManager restart. */
        fun stopForLogout(context: Context) = stop(context)

        private fun isAppInForeground(): Boolean = try {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (_: Exception) {
            false
        }

        private fun isDataSyncQuotaException(e: Exception): Boolean {
            val msg = e.message.orEmpty()
            return msg.contains("Time limit already exhausted", ignoreCase = true) ||
                msg.contains("foreground service type dataSync", ignoreCase = true)
        }
    }
}
