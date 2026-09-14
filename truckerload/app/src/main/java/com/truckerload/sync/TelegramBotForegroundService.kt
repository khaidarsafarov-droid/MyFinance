package com.truckerload.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.TelegramTokenStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotBranding
import com.truckerload.data.remote.TelegramBotTokenFingerprint
import com.truckerload.utils.LogRedactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-process Telegram long-poll while the app is visible.
 * No Android foreground service — Play does not need DATA_SYNC FGS.
 * Background catch-up is [TelegramSyncWorker] (~15 min).
 */
object TelegramBotForegroundService {
    private const val TAG = "TelegramBotPoller"
    private const val KEY_BOT_FEATURES_SETUP = "bot_features_setup_v4"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunningFlag = AtomicBoolean(false)
    private val startRequested = AtomicBoolean(false)
    private var pollJob: Job? = null

    fun isRunning(): Boolean = isRunningFlag.get()

    fun canStart(context: Context): Boolean {
        val userId = AuthStore(context).currentUserIdOrNull() ?: return false
        return TelegramTokenStore(context, userId).getToken().isNotBlank()
    }

    fun start(context: Context) {
        if (!canStart(context)) return
        val app = context.applicationContext
        if (isRunningFlag.get() || TelegramPollCoordinator.isForegroundPolling()) return
        if (!startRequested.compareAndSet(false, true)) return
        val userId = AuthStore(app).currentUserIdOrNull()
        val token = userId?.let { TelegramTokenStore(app, it).getToken() }.orEmpty()
        if (userId == null || token.isBlank()) {
            startRequested.set(false)
            return
        }
        startRequested.set(false)
        isRunningFlag.set(true)
        setupBotFeaturesOnce(app, token)
        pollJob?.cancel()
        TelegramPollCoordinator.markForegroundPolling(true)
        pollJob = scope.launch {
            try {
                pollLoop(app, token, userId)
            } finally {
                TelegramPollCoordinator.markForegroundPolling(false)
                isRunningFlag.set(false)
            }
        }
    }

    fun stop(@Suppress("UNUSED_PARAMETER") context: Context) {
        startRequested.set(false)
        isRunningFlag.set(false)
        pollJob?.cancel()
        TelegramPollCoordinator.markForegroundPolling(false)
    }

    fun stopForLogout(context: Context) = stop(context)

    private fun setupBotFeaturesOnce(app: Context, token: String) {
        val prefs = app.getSharedPreferences(TelegramSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
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
                    prefs.edit { putString(KEY_BOT_FEATURES_SETUP, tokenFp) }
                }.onFailure { e -> Log.w(TAG, "setMyCommands: ${LogRedactor.redact(e.message)}") }
                api.setChatMenuButton().onFailure { e ->
                    Log.w(TAG, "setChatMenuButton: ${LogRedactor.redact(e.message)}")
                }
            }
            TelegramBotBranding.apply(app, token)
        }
    }

    private fun storedTokenFingerprint(prefs: SharedPreferences, key: String): String =
        prefs.all[key] as? String ?: ""

    private suspend fun pollLoop(app: Context, token: String, expectedUserId: String) {
        val engine = TelegramBotSyncEngine(app)
        while (scope.isActive && isRunningFlag.get()) {
            try {
                val activeUserId = AuthStore(app).currentUserIdOrNull()
                if (activeUserId.isNullOrBlank() || activeUserId != expectedUserId) {
                    Log.w(TAG, "Active user changed — stopping pollLoop")
                    break
                }
                val activeToken = TelegramTokenStore(app, activeUserId).getToken()
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
}
