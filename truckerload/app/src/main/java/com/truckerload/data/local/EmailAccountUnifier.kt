package com.truckerload.data.local

import android.content.Context
import android.util.Log
import com.truckerload.data.preferences.AccountIds

/**
 * Email/password Room id is a Supabase UUID after cloud login, but offline
 * fallback used `local_<email-hash>`. Copy the local-hash journal onto the
 * UUID file when the UUID DB is still empty so one email stays one journal.
 */
object EmailAccountUnifier {
    private const val TAG = "EmailAccountUnifier"

    fun relocateLocalEmailJournal(
        context: Context,
        canonicalUserId: String,
        email: String?,
    ): Boolean {
        val canonical = canonicalUserId.trim()
        if (canonical.isBlank() ||
            canonical == AccountIds.LOCAL_DEV ||
            canonical.startsWith("google_") ||
            canonical.startsWith("local_")
        ) {
            return false
        }
        val mail = email?.trim().orEmpty()
        if (mail.isBlank()) return false
        val alias = AccountIds.fromEmail(mail)
        if (alias == canonical) return false
        val app = context.applicationContext
        val target = app.getDatabasePath(AppDatabase.databaseNameFor(canonical))
        if (target.exists() && DatabaseFileCopy.isHealthyDatabase(target)) return false
        val source = app.getDatabasePath(AppDatabase.databaseNameFor(alias))
        if (!source.exists()) return false
        AppDatabase.closeCurrent()
        if (target.exists()) DatabaseFileCopy.deleteDbTree(target)
        val result = DatabaseFileCopy.copyWithSidecars(source, target)
        if (result.isFailure) {
            Log.e(TAG, "Failed to unify $alias → $canonical", result.exceptionOrNull())
            return false
        }
        Log.i(TAG, "Unified email journal $alias → $canonical")
        return true
    }
}
