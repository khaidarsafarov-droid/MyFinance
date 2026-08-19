package com.truckerload.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.truckerload.data.preferences.AccountIds

/**
 * One Google account → one TruckerLoad login (`google_<hash>` Room + prefs).
 *
 * Older builds keyed the same Google user by a Supabase UUID. On the next
 * session we copy that alias journal into the canonical Google id (never the
 * other way around, never `local_dev` / `truckerload_db` / another Google id).
 */
object GoogleAccountUnifier {
    private const val TAG = "GoogleAccountUnifier"

    private val ACCOUNT_PREF_PREFIXES = listOf(
        "truckerload_user_profile_",
        "truckerload_rpm_",
        "truckerload_last_defaults_",
        "truckerload_widget_goals_",
        "truckerload_stats_selection_",
        "truckerload_selected_state_",
        "truckerload_widget_",
        "google_drive_backup_",
        "truckerload_cloud_sync_",
        "truckerload_media_sync_",
    )

    /**
     * Returns the session userId to keep. Relocates [persistedUserId] → canonical
     * Google id when a Google `sub` is present and the alias file still exists.
     */
    fun canonicalSessionUserId(
        context: Context,
        persistedUserId: String,
        googleSub: String?,
    ): String {
        val persisted = persistedUserId.trim()
        val sub = googleSub?.trim().orEmpty()
        if (sub.isBlank() || persisted.startsWith("google_")) return persisted
        val canonical = AccountIds.fromGoogleSub(sub)
        relocateAliases(context, canonical, listOf(persisted))
        val target = context.applicationContext.getDatabasePath(AppDatabase.databaseNameFor(canonical))
        val source = context.applicationContext.getDatabasePath(AppDatabase.databaseNameFor(persisted))
        return when {
            target.exists() && DatabaseFileCopy.isHealthyDatabase(target) -> canonical
            source.exists() -> persisted
            else -> canonical
        }
    }

    fun relocateAliases(
        context: Context,
        canonicalUserId: String,
        aliasUserIds: Collection<String>,
    ): Boolean {
        val canonical = canonicalUserId.trim()
        if (!canonical.startsWith("google_")) return false
        val aliases = aliasUserIds.map { it.trim() }.filter { isRelocatableAlias(it, canonical) }
        if (aliases.isEmpty()) return false
        val app = context.applicationContext
        var changed = false
        if (copyJournalIfCanonicalEmpty(app, canonical, aliases)) {
            changed = true
        }
        for (alias in aliases) {
            if (copyAccountPrefsIfEmpty(app, alias, canonical)) {
                changed = true
            }
        }
        return changed
    }

    private fun isRelocatableAlias(alias: String, canonical: String): Boolean {
        if (alias.isBlank() || alias == canonical) return false
        if (alias == AccountIds.LOCAL_DEV) return false
        if (alias == LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE) return false
        // Email-hash and other Google ids are different logins — never auto-merge.
        if (alias.startsWith("local_") || alias.startsWith("google_")) return false
        return true
    }

    private fun copyJournalIfCanonicalEmpty(
        app: Context,
        canonical: String,
        aliases: List<String>,
    ): Boolean {
        val target = app.getDatabasePath(AppDatabase.databaseNameFor(canonical))
        if (target.exists() && DatabaseFileCopy.isHealthyDatabase(target)) return false
        val sourceId = aliases.firstOrNull { alias ->
            app.getDatabasePath(AppDatabase.databaseNameFor(alias)).exists()
        } ?: return false
        AppDatabase.closeCurrent()
        if (target.exists()) DatabaseFileCopy.deleteDbTree(target)
        val source = app.getDatabasePath(AppDatabase.databaseNameFor(sourceId))
        val result = DatabaseFileCopy.copyWithSidecars(source, target)
        if (result.isFailure) {
            Log.e(TAG, "Failed to unify $sourceId → $canonical", result.exceptionOrNull())
            return false
        }
        Log.i(TAG, "Unified Google journal $sourceId → $canonical")
        return true
    }

    private fun copyAccountPrefsIfEmpty(app: Context, sourceId: String, canonical: String): Boolean {
        val from = AccountIds.sanitizeFilePart(sourceId)
        val to = AccountIds.sanitizeFilePart(canonical)
        var copied = false
        for (prefix in ACCOUNT_PREF_PREFIXES) {
            val source = app.getSharedPreferences(prefix + from, Context.MODE_PRIVATE)
            val values = source.all
            if (values.isEmpty()) continue
            val dest = app.getSharedPreferences(prefix + to, Context.MODE_PRIVATE)
            if (dest.all.isNotEmpty()) continue
            dest.edit(commit = true) {
                values.forEach { (key, value) -> putPrefValue(key, value) }
            }
            copied = true
        }
        return copied
    }

    @Suppress("UNCHECKED_CAST")
    private fun SharedPreferences.Editor.putPrefValue(key: String, value: Any?) {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            is Set<*> -> putStringSet(key, value as Set<String>)
        }
    }
}
