package com.truckerload.data.local

import android.content.Context
import android.util.Log
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.utils.CrashReporting

/**
 * Cross-account local DB absorb requires explicit user consent.
 *
 * Previously [AppDatabase] auto-copied `local_dev` / email-hash DB into the first
 * cloud login on a shared device, which could attach the wrong user's journal.
 */
object LegacyDatabaseAbsorb {
    private const val TAG = "LegacyDbAbsorb"
    const val META_PREFS = "truckerload_account_meta"
    const val KEY_LEGACY_DB_CLAIMED = "legacy_db_claimed_by_account"
    const val KEY_LEGACY_DB_OWNER = "legacy_db_owner"
    private const val KEY_PENDING_SOURCE = "legacy_db_pending_absorb_source"
    private const val KEY_PENDING_USER = "legacy_db_pending_absorb_user"
    private const val KEY_ABSORB_DECLINED = "legacy_db_absorb_declined"

    /**
     * Called from [AppDatabase.getInstance]: detect a candidate and mark pending —
     * do **not** copy until [acceptAndCopy].
     */
    fun notePendingIfNeeded(context: Context, userId: String) {
        if (userId == AccountIds.LOCAL_DEV) return
        val app = context.applicationContext
        val meta = app.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (meta.getBoolean(KEY_LEGACY_DB_CLAIMED, false)) return
        if (meta.getBoolean(KEY_ABSORB_DECLINED, false)) return

        val target = app.getDatabasePath(AppDatabase.databaseNameFor(userId))
        if (target.exists() && DatabaseFileCopy.isHealthyDatabase(target)) {
            meta.edit()
                .putBoolean(KEY_LEGACY_DB_CLAIMED, true)
                .putString(KEY_LEGACY_DB_OWNER, userId)
                .remove(KEY_PENDING_SOURCE)
                .remove(KEY_PENDING_USER)
                .apply()
            return
        }

        val sourceId = findCandidateSourceId(app, userId) ?: return
        meta.edit()
            .putString(KEY_PENDING_SOURCE, sourceId)
            .putString(KEY_PENDING_USER, userId)
            .apply()
        Log.i(TAG, "Pending absorb prompt for user=$userId from source=$sourceId")
    }

    fun hasPendingPrompt(context: Context, userId: String): Boolean {
        val meta = context.applicationContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        if (meta.getBoolean(KEY_LEGACY_DB_CLAIMED, false)) return false
        if (meta.getBoolean(KEY_ABSORB_DECLINED, false)) return false
        return meta.getString(KEY_PENDING_USER, null) == userId &&
            !meta.getString(KEY_PENDING_SOURCE, null).isNullOrBlank()
    }

    fun pendingSourceLabel(context: Context): String? {
        val meta = context.applicationContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        return meta.getString(KEY_PENDING_SOURCE, null)
    }

    /**
     * Copies the pending source DB into [userId]'s file after closing the Room pool.
     * Caller must rebuild the user session afterward.
     */
    fun acceptAndCopy(context: Context, userId: String): Boolean {
        val app = context.applicationContext
        val meta = app.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        val sourceId = meta.getString(KEY_PENDING_SOURCE, null) ?: return false
        if (meta.getString(KEY_PENDING_USER, null) != userId) return false

        AppDatabase.closeCurrent()
        val target = app.getDatabasePath(AppDatabase.databaseNameFor(userId))
        if (target.exists()) {
            DatabaseFileCopy.deleteDbTree(target)
        }
        val source = app.getDatabasePath(AppDatabase.databaseNameFor(sourceId))
        if (!source.exists()) {
            decline(app, userId)
            return false
        }
        val result = DatabaseFileCopy.copyWithSidecars(source, target)
        if (result.isFailure) {
            val error = result.exceptionOrNull()
                ?: IllegalStateException("absorb_previous failed")
            Log.e(TAG, "absorb failed for user=$userId", error)
            CrashReporting.setCustomKey("db_copy_op", "absorb_previous_consent")
            CrashReporting.setCustomKey("legacy_copy_user", userId)
            CrashReporting.recordException(error)
            return false
        }
        meta.edit()
            .putBoolean(KEY_LEGACY_DB_CLAIMED, true)
            .putString(KEY_LEGACY_DB_OWNER, userId)
            .remove(KEY_PENDING_SOURCE)
            .remove(KEY_PENDING_USER)
            .apply()
        Log.i(TAG, "Absorb accepted: $sourceId → $userId")
        return true
    }

    fun decline(context: Context, userId: String) {
        val meta = context.applicationContext.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
        meta.edit()
            .putBoolean(KEY_ABSORB_DECLINED, true)
            .putBoolean(KEY_LEGACY_DB_CLAIMED, true)
            .putString(KEY_LEGACY_DB_OWNER, userId)
            .remove(KEY_PENDING_SOURCE)
            .remove(KEY_PENDING_USER)
            .apply()
        Log.i(TAG, "Absorb declined for user=$userId")
    }

    fun findCandidateSourceId(context: Context, userId: String): String? {
        val app = context.applicationContext
        val email = AuthStore(app).email.value
        val candidates = buildList {
            add(AccountIds.LOCAL_DEV)
            if (email.isNotBlank()) {
                add(AccountIds.fromEmail(email))
            }
        }
        return candidates.firstOrNull { candidate ->
            candidate != userId &&
                app.getDatabasePath(AppDatabase.databaseNameFor(candidate)).exists()
        }
    }
}
