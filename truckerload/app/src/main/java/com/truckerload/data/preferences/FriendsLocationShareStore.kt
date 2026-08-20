package com.truckerload.data.preferences

import android.content.Context
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import com.truckerload.domain.friends.FriendsLocationSharePolicy.Motion

/**
 * Process-safe runtime state for friends location sharing (activity, last publish,
 * live-session deadline). Account-keyed so two users on one device do not mix.
 *
 * Preferences (interval / live toggle) live in [SettingsDataStore].
 */
class FriendsLocationShareStore(context: Context) {

    private val app = context.applicationContext

    private fun prefs() =
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun accountPart(): String =
        AuthStore(app).currentUserIdOrNull()?.let(AccountIds::sanitizeFilePart) ?: "none"

    fun lastMotion(): Motion {
        val raw = prefs().getString(key(KEY_MOTION), Motion.UNKNOWN.name) ?: Motion.UNKNOWN.name
        return runCatching { Motion.valueOf(raw) }.getOrDefault(Motion.UNKNOWN)
    }

    fun setLastMotion(motion: Motion) {
        prefs().edit().putString(key(KEY_MOTION), motion.name).apply()
    }

    fun lastPublishedAtMs(): Long = prefs().getLong(key(KEY_PUBLISHED), 0L)

    fun markPublished(nowMs: Long = System.currentTimeMillis()) {
        prefs().edit().putLong(key(KEY_PUBLISHED), nowMs).apply()
    }

    fun liveUntilMs(): Long = prefs().getLong(key(KEY_LIVE_UNTIL), 0L)

    fun setLiveUntilMs(untilMs: Long) {
        prefs().edit().putLong(key(KEY_LIVE_UNTIL), untilMs).apply()
    }

    fun clearLiveUntil() {
        prefs().edit().remove(key(KEY_LIVE_UNTIL)).apply()
    }

    fun presenceAccuracySupported(): Boolean? {
        if (!prefs().contains(key(KEY_ACCURACY_OK))) return null
        return prefs().getBoolean(key(KEY_ACCURACY_OK), false)
    }

    fun setPresenceAccuracySupported(supported: Boolean) {
        prefs().edit().putBoolean(key(KEY_ACCURACY_OK), supported).apply()
    }

    fun isLiveSessionActive(nowMs: Long = System.currentTimeMillis()): Boolean =
        FriendsLocationSharePolicy.liveSessionActive(liveUntilMs(), nowMs)

    private fun key(base: String) = "${base}_${accountPart()}"

    companion object {
        private const val PREFS = "friends_location_share_runtime"
        private const val KEY_MOTION = "motion"
        private const val KEY_PUBLISHED = "published_at"
        private const val KEY_LIVE_UNTIL = "live_until"
        private const val KEY_ACCURACY_OK = "accuracy_col"
    }
}
