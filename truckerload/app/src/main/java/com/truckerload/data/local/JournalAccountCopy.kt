package com.truckerload.data.local

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.data.preferences.AccountIds
import java.io.File

/**
 * Account-scoped files that sit beside the Room journal. Copying only the
 * database would drop the weekly goal, Drive link, bot archive, and backups.
 */
internal object JournalAccountCopy {
    private val prefPrefixes = listOf(
        "truckerload_consent_",
        "truckerload_last_defaults_",
        "truckerload_reg_progress_",
        "truckerload_rpm_",
        "truckerload_selected_state_",
        "truckerload_user_profile_",
        "truckerload_widget_goals_",
        "truckerload_widget_",
        "truckerload_deleted_loads_",
        "google_drive_backup_",
        "telegram_sync_",
        "backup_companion_",
    )

    fun copyScopedState(context: Context, sourceUserId: String?, targetUserId: String) {
        val sourcePart = sourceUserId?.let(AccountIds::sanitizeFilePart) ?: return
        val targetPart = AccountIds.sanitizeFilePart(targetUserId)
        if (sourcePart == targetPart) return
        prefPrefixes.forEach { prefix ->
            copyPrefsIfTargetEmpty(context, prefix + sourcePart, prefix + targetPart)
        }
        copyFileIfMissing(
            File(context.filesDir, "telegram_message_archive_$sourcePart.jsonl"),
            File(context.filesDir, "telegram_message_archive_$targetPart.jsonl"),
        )
        val external = context.getExternalFilesDir(null) ?: return
        copyTreeIfMissing(
            File(external, "backups/$sourcePart"),
            File(external, "backups/$targetPart"),
        )
    }

    private fun copyPrefsIfTargetEmpty(context: Context, sourceName: String, targetName: String) {
        val source = context.getSharedPreferences(sourceName, Context.MODE_PRIVATE)
        if (source.all.isEmpty()) return
        val target = context.getSharedPreferences(targetName, Context.MODE_PRIVATE)
        if (target.all.isNotEmpty()) return
        target.edit().apply {
            copyEntries(source, this)
            commit()
        }
    }

    private fun copyEntries(source: SharedPreferences, editor: SharedPreferences.Editor) {
        source.all.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
    }

    private fun copyFileIfMissing(source: File, target: File) {
        if (!source.isFile || source.length() == 0L || target.exists()) return
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = false)
    }

    private fun copyTreeIfMissing(source: File, target: File) {
        if (!source.exists() || target.exists()) return
        source.copyRecursively(target, overwrite = false)
    }
}
