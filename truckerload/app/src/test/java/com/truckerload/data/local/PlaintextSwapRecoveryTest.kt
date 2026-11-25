package com.truckerload.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PlaintextSwapRecoveryTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun crashBetweenTheTwoRenamesPutsThePlaintextDatabaseBack() {
        val db = File(folder.root, "truckerload_user")
        val backup = File(db.path + ".plaintext-bak").apply { writeText("plain rows") }
        val staged = File(db.path + ".encrypting").apply { writeText("half") }

        PlaintextDatabaseEncryptor.recoverInterruptedSwap(db)

        assertEquals("plain rows", db.readText())
        assertFalse(backup.exists())
        assertFalse(staged.exists())
    }

    @Test
    fun existingDatabaseIsNeverReplacedByTheBackup() {
        val db = File(folder.root, "truckerload_user").apply { writeText("current") }
        val backup = File(db.path + ".plaintext-bak").apply { writeText("stale") }

        PlaintextDatabaseEncryptor.recoverInterruptedSwap(db)

        assertEquals("current", db.readText())
        assertTrue(backup.exists())
    }
}
