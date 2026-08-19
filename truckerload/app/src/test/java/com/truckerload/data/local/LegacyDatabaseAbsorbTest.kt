package com.truckerload.data.local

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LegacyDatabaseAbsorbTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(LegacyDatabaseAbsorb.META_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        // Clean leftover DB files from prior tests.
        listOf("local_dev", "cloud-user-1", "cloud-user-2").forEach { id ->
            val f = context.getDatabasePath(AppDatabase.databaseNameFor(id))
            DatabaseFileCopy.deleteDbTree(f)
        }
        DatabaseFileCopy.deleteDbTree(context.getDatabasePath(LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE))
    }

    @Test
    fun notePending_setsPromptWhenLocalDevDbExists() {
        val source = context.getDatabasePath(AppDatabase.databaseNameFor("local_dev"))
        source.parentFile?.mkdirs()
        source.writeBytes(ByteArray(64))

        assertEquals("local_dev", LegacyDatabaseAbsorb.findCandidateSourceId(context, "cloud-user-1"))
        LegacyDatabaseAbsorb.notePendingIfNeeded(context, "cloud-user-1")
        assertTrue(LegacyDatabaseAbsorb.hasPendingPrompt(context, "cloud-user-1"))
        assertEquals("local_dev", LegacyDatabaseAbsorb.pendingSourceLabel(context))
    }

    @Test
    fun decline_clearsPendingAndDoesNotAskAgain() {
        val source = context.getDatabasePath(AppDatabase.databaseNameFor("local_dev"))
        source.parentFile?.mkdirs()
        source.writeBytes(ByteArray(64))

        LegacyDatabaseAbsorb.notePendingIfNeeded(context, "cloud-user-2")
        assertTrue(LegacyDatabaseAbsorb.hasPendingPrompt(context, "cloud-user-2"))

        LegacyDatabaseAbsorb.decline(context, "cloud-user-2")
        assertFalse(LegacyDatabaseAbsorb.hasPendingPrompt(context, "cloud-user-2"))

        LegacyDatabaseAbsorb.notePendingIfNeeded(context, "cloud-user-2")
        assertFalse(
            "Declined absorb must not re-prompt",
            LegacyDatabaseAbsorb.hasPendingPrompt(context, "cloud-user-2"),
        )
    }

    @Test
    fun notePending_setsPromptWhenLegacySingleFileExists() {
        val source = context.getDatabasePath(LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE)
        source.parentFile?.mkdirs()
        source.writeBytes(ByteArray(64))

        assertEquals(
            LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE,
            LegacyDatabaseAbsorb.findCandidateSourceId(context, "cloud-user-1"),
        )
        LegacyDatabaseAbsorb.notePendingIfNeeded(context, "cloud-user-1")
        assertTrue(LegacyDatabaseAbsorb.hasPendingPrompt(context, "cloud-user-1"))
        assertEquals(
            LegacyDatabaseAbsorb.LEGACY_SINGLE_FILE,
            LegacyDatabaseAbsorb.pendingSourceLabel(context),
        )
    }
}
