package com.truckerload.utils

import com.truckerload.data.backup.BackupData
import com.truckerload.data.backup.BackupDataCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AtomicFileWriteTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun replacesContentAndLeavesNoTempFile() {
        val target = folder.newFile("auto_backup_1.tlb")
        target.writeText("old")

        target.writeTextAtomically("new content")

        assertEquals("new content", target.readText())
        assertFalse(java.io.File(target.path + ".tmp").exists())
    }

    @Test
    fun createsMissingParentDirectory() {
        val target = java.io.File(folder.root, "a/b/backup.tlb")
        target.writeTextAtomically("x")
        assertTrue(target.isFile)
    }

    @Test
    fun truncatedBackupIsNotDecodable() {
        val good = BackupDataCodec.toJson(BackupData(accountId = "u"))
        val torn = folder.newFile("torn.tlb").apply { writeText(good.take(good.length / 2)) }
        val whole = folder.newFile("whole.tlb").apply { writeText(good) }

        assertFalse(BackupService.isDecodableBackup(torn))
        assertTrue(BackupService.isDecodableBackup(whole))
    }
}
