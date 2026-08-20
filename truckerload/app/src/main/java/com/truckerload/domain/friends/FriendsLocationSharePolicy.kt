package com.truckerload.domain.friends

/**
 * Battery policy for friends location sharing.
 *
 * Background updates go through WorkManager (min 15 min). Foreground service
 * is only for a time-boxed live session.
 */
object FriendsLocationSharePolicy {
    const val DEFAULT_INTERVAL_MINUTES = 30
    const val STILL_INTERVAL_MINUTES = 60
    const val LIVE_SESSION_MS = 15 * 60_000L
    const val LIVE_GPS_PERIOD_MS = 30_000L
    const val FCM_WATCH_TYPE = "friends_watch"

    val allowedIntervalsMinutes: IntArray = intArrayOf(15, 30, 60)

    enum class Motion {
        STILL,
        MOVING,
        UNKNOWN,
    }

    fun clampIntervalMinutes(raw: Int): Int = when {
        raw <= 15 -> 15
        raw >= 60 -> 60
        else -> 30
    }

    /** STILL stretches the period to 60 min; driving keeps the user choice (min 15). */
    fun effectiveIntervalMinutes(userMinutes: Int, motion: Motion): Int {
        val clamped = clampIntervalMinutes(userMinutes)
        return if (motion == Motion.STILL) {
            maxOf(clamped, STILL_INTERVAL_MINUTES)
        } else {
            clamped
        }
    }

    fun motionFromActivityType(activityType: Int): Motion = when (activityType) {
        // DetectedActivity.STILL
        3 -> Motion.STILL
        // IN_VEHICLE, ON_BICYCLE, ON_FOOT, WALKING, RUNNING
        0, 1, 2, 7, 8 -> Motion.MOVING
        else -> Motion.UNKNOWN
    }

    /**
     * Skip a fresh GPS fix when we already published recently while stationary.
     * [nowMs] / [lastPublishedAtMs] are epoch millis; lastPublishedAtMs=0 means never.
     */
    fun shouldSkipStationaryFix(
        motion: Motion,
        lastPublishedAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (motion != Motion.STILL || lastPublishedAtMs <= 0L) return false
        return nowMs - lastPublishedAtMs < STILL_INTERVAL_MINUTES * 60_000L
    }

    fun liveSessionActive(liveUntilMs: Long, nowMs: Long): Boolean =
        liveUntilMs > nowMs

    fun liveUntilFromNow(nowMs: Long): Long = nowMs + LIVE_SESSION_MS

    fun workFlexMinutes(intervalMinutes: Int): Long {
        val clamped = clampIntervalMinutes(intervalMinutes)
        return (clamped / 3).coerceAtLeast(5).toLong()
    }
}
