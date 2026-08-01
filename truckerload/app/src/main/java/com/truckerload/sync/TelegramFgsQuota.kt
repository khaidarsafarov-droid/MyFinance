package com.truckerload.sync

/**
 * Tracks Android 15+ [dataSync] foreground-service quota exhaustion.
 *
 * After [Service.onTimeout], the platform forbids starting another dataSync FGS until
 * the user brings the app to the foreground (timer reset) or the 24h window rolls.
 * Aggressive AlarmManager / WorkManager restarts must wait — otherwise start fails or
 * the process crashes again with ForegroundServiceDidNotStopInTimeException.
 */
object TelegramFgsQuota {
    @Volatile
    private var pausedAfterTimeout = false

    fun markTimedOut() {
        pausedAfterTimeout = true
    }

    fun clearPause() {
        pausedAfterTimeout = false
    }

    fun isPaused(): Boolean = pausedAfterTimeout

    /** Test / process-reset helper. */
    internal fun resetForTests() {
        pausedAfterTimeout = false
    }
}
