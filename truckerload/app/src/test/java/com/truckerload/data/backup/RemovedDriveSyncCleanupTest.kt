package com.truckerload.data.backup

import android.content.Context
import androidx.core.content.edit
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SecurePreferences
import java.io.File
import org.junit.After
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
@Config(sdk = [34])
class RemovedDriveSyncCleanupTest {

    @Before
    fun setUp() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @After
    fun tearDown() {
        AuthStore.resetForTests()
        SecurePreferences.resetFallbackForTests()
    }

    @Test
    fun matchesOnlyRemovedSyncPrefs() {
        assertTrue(RemovedDriveSyncCleanup.isRemovedSyncPrefs("google_drive_backup"))
        assertTrue(RemovedDriveSyncCleanup.isRemovedSyncPrefs("google_drive_backup_local_dev"))
        assertTrue(RemovedDriveSyncCleanup.isRemovedSyncPrefs("truckerload_auth_credentials_enc"))
        assertTrue(RemovedDriveSyncCleanup.isRemovedSyncPrefs("com.google.android.gms.signin"))
        assertFalse(RemovedDriveSyncCleanup.isRemovedSyncPrefs("truckerload_account_meta"))
        assertFalse(RemovedDriveSyncCleanup.isRemovedSyncPrefs("truckerload_auth"))
        assertFalse(RemovedDriveSyncCleanup.isRemovedSyncPrefs("user_profile_local_dev"))
    }

    @Test
    fun wipesDriveAndSignInPrefsButKeepsEverythingElse() {
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("google_drive_backup_local_dev", Context.MODE_PRIVATE)
            .edit(commit = true) { putString("account_email", "driver@example.com") }
        app.getSharedPreferences("com.google.android.gms.signin", Context.MODE_PRIVATE)
            .edit(commit = true) { putString("defaultGoogleSignInAccount", "driver@example.com") }
        val keep = app.getSharedPreferences("journal_settings_keep", Context.MODE_PRIVATE)
        keep.edit(commit = true) { putString("week_start", "SUNDAY") }
        val photo = File(app.filesDir, "photos/load.jpg").apply {
            parentFile?.mkdirs()
            writeText("jpeg")
        }

        RemovedDriveSyncCleanup.runOnce(app)

        assertTrue(
            app.getSharedPreferences("google_drive_backup_local_dev", Context.MODE_PRIVATE).all.isEmpty(),
        )
        assertTrue(
            app.getSharedPreferences("com.google.android.gms.signin", Context.MODE_PRIVATE).all.isEmpty(),
        )
        assertEquals("SUNDAY", keep.getString("week_start", null))
        assertTrue(photo.isFile)
    }

    @Test
    fun runsOnlyOnce() {
        val app = RuntimeEnvironment.getApplication()
        RemovedDriveSyncCleanup.runOnce(app)
        app.getSharedPreferences("google_drive_backup_local_dev", Context.MODE_PRIVATE)
            .edit(commit = true) { putString("account_email", "later@example.com") }

        RemovedDriveSyncCleanup.runOnce(app)

        assertEquals(
            "later@example.com",
            app.getSharedPreferences("google_drive_backup_local_dev", Context.MODE_PRIVATE)
                .getString("account_email", null),
        )
    }
}
