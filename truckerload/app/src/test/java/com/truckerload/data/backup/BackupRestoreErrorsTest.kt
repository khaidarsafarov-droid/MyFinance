package com.truckerload.data.backup

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRestoreErrorsTest {

    @Test
    fun chartNote_and_badFormat_haveDistinctMessages() {
        val context = RuntimeEnvironment.getApplication()
        val chart = BackupRestoreErrors.userMessage(
            context,
            BackupRestoreException.ChartNoteNotBackup(),
        )
        val bad = BackupRestoreErrors.userMessage(
            context,
            BackupRestoreException.InvalidFormat(),
        )
        val corrupted = BackupRestoreErrors.userMessage(
            context,
            BackupRestoreException.Corrupted(),
        )
        val tooNew = BackupRestoreErrors.userMessage(
            context,
            BackupRestoreException.SchemaTooNew(99),
        )
        assertEquals(context.getString(com.truckerload.R.string.backup_restore_chart_note), chart)
        assertEquals(context.getString(com.truckerload.R.string.backup_restore_bad_format), bad)
        assertEquals(context.getString(com.truckerload.R.string.backup_restore_corrupted), corrupted)
        assertEquals(context.getString(com.truckerload.R.string.backup_restore_schema_too_new), tooNew)
    }
}
