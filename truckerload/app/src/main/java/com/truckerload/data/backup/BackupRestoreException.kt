package com.truckerload.data.backup

import com.truckerload.R

/**
 * Typed restore failures. Messages never include file paths or backup JSON.
 */
sealed class BackupRestoreException : IllegalStateException() {
    abstract val messageResId: Int
    open val formatArgs: Array<Any> = emptyArray()

    class ReadFailed : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_read_failed
    }

    class InvalidFormat : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_bad_format
    }

    class Corrupted : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_corrupted
    }

    class ChartNoteNotBackup : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_chart_note
    }

    class SchemaTooNew(val fileVersion: Int) : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_schema_too_new
    }

    class WrongAccount : BackupRestoreException() {
        override val messageResId: Int = R.string.backup_restore_wrong_account
    }
}
