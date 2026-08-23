package com.truckerload.data.backup

import android.content.Context
import com.truckerload.R

object BackupRestoreErrors {
    fun userMessage(context: Context, error: Throwable): String {
        if (error is BackupRestoreException) {
            return context.getString(error.messageResId, *error.formatArgs)
        }
        val detail = error.message?.trim().orEmpty()
        return if (detail.isNotEmpty()) {
            context.getString(R.string.settings_restore_error, detail)
        } else {
            context.getString(R.string.backup_restore_bad_format)
        }
    }
}
