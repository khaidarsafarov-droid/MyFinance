package com.truckerload.data.backup

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GoogleDriveBackupPrefsIsolationTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun prefsAreIsolatedAcrossUserIds() {
        val a = GoogleDriveBackupPrefs(context, userId = "user_a")
        val b = GoogleDriveBackupPrefs(context, userId = "user_b")
        a.driveFileId = "file-a"
        a.accountEmail = "a@example.com"
        b.driveFileId = "file-b"
        b.accountEmail = "b@example.com"

        assertEquals("file-a", GoogleDriveBackupPrefs(context, "user_a").driveFileId)
        assertEquals("file-b", GoogleDriveBackupPrefs(context, "user_b").driveFileId)
        assertNotEquals(
            GoogleDriveBackupPrefs.prefsName("user_a"),
            GoogleDriveBackupPrefs.prefsName("user_b"),
        )
    }

    @Test
    fun clearDoesNotWipeOtherUser() {
        val a = GoogleDriveBackupPrefs(context, userId = "user_a")
        val b = GoogleDriveBackupPrefs(context, userId = "user_b")
        a.driveFileId = "file-a"
        b.driveFileId = "file-b"
        a.clear()
        assertNull(GoogleDriveBackupPrefs(context, "user_a").driveFileId)
        assertEquals("file-b", GoogleDriveBackupPrefs(context, "user_b").driveFileId)
    }
}
