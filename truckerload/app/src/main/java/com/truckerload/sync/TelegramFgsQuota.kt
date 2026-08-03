package com.truckerload.sync

import android.content.Context
import androidx.core.content.edit

/**
 * Tracks Android 15+ [dataSync] foreground-service quota exhaustion.
 *
 * After [android.app.Service.onTimeout], the platform forbids starting another dataSync FGS until
 * the user brings the app to the foreground (timer reset) or the 24h window rolls.
 * Aggressive AlarmManager / WorkManager restarts must wait — otherwise start fails or
 * the process crashes again with ForegroundServiceDidNotStopInTimeException.
 *
 * Pause state is persisted so process death cannot clear it while the platform quota
 * is still exhausted.
 */
object TelegramFgsQuota {
    private const val PREFS = "telegram_fgs_quota"
    private const val KEY_PAUSED = "paused_after_timeout"
    private const val KEY_PAUSED_AT = "paused_at_ms"
    /** Safety TTL if the user never foregrounds the app (matches ~24h quota window). */
    private const val PAUSE_TTL_MS = 24L * 60 * 60 * 1000

    @Volatile
    private var pausedAfterTimeout = false

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val paused = prefs.getBoolean(KEY_PAUSED, false)
        val at = prefs.getLong(KEY_PAUSED_AT, 0L)
        pausedAfterTimeout = if (paused && at > 0L && System.currentTimeMillis() - at < PAUSE_TTL_MS) {
            true
        } else {
            if (paused) prefs.edit { clear() }
            false
        }
    }

    fun markTimedOut(context: Context? = appContext) {
        pausedAfterTimeout = true
        // FIX: durable pause — in-memory flag alone reset on process death and caused restart crashes
        val ctx = context?.applicationContext ?: return
        appContext = ctx
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_PAUSED, true)
            putLong(KEY_PAUSED_AT, System.currentTimeMillis())
        }
    }

    fun clearPause(context: Context? = appContext) {
        pausedAfterTimeout = false
        val ctx = context?.applicationContext ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    fun isPaused(context: Context? = appContext): Boolean {
        if (!pausedAfterTimeout) {
            // Lazy hydrate after process death when init() was not called yet.
            val ctx = context?.applicationContext
            if (ctx != null) {
                init(ctx)
            }
        }
        if (!pausedAfterTimeout) return false
        val ctx = context?.applicationContext ?: appContext ?: return pausedAfterTimeout
        val at = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_PAUSED_AT, 0L)
        if (at > 0L && System.currentTimeMillis() - at >= PAUSE_TTL_MS) {
            clearPause(ctx)
            return false
        }
        return true
    }

    /** Test / process-reset helper. */
    internal fun resetForTests() {
        pausedAfterTimeout = false
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit { clear() }
    }
}
