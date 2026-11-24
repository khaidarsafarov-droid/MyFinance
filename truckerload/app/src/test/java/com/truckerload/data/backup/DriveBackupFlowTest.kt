package com.truckerload.data.backup

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DriveBackupFlowTest {

    private lateinit var context: Context
    private lateinit var prefs: GoogleDriveBackupPrefs

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        prefs = GoogleDriveBackupService.prefs(context)
        prefs.clear()
        prefs.accountEmail = "driver@example.com"
    }

    @Test
    fun backupNow_uploadsJsonAndClearsError() = runBlocking {
        val store = FakeDriveRemoteStore()
        val json = BackupDataCodec.toJson(BackupTestFixtures.sampleBackup())
        prefs.lastSyncError = "previous"
        val result = flow(store, createJson = { json }).backupNow(context)
        assertTrue(result.isSuccess)
        assertEquals(listOf(json), store.uploaded)
        assertEquals(json, store.remoteJson)
        assertTrue(prefs.lastSyncAt > 0L)
        assertNull(prefs.lastSyncError)
        assertEquals(0L, prefs.lastSyncErrorAt)
        assertTrue(result.getOrThrow().contains("driver@example.com"))
    }

    @Test
    fun backupNow_failsWhenNotLinked() = runBlocking {
        prefs.accountEmail = null
        val store = FakeDriveRemoteStore()
        val result = flow(store, linked = false, createJson = { "{}" }).backupNow(context)
        assertTrue(result.isFailure)
        assertTrue(store.uploaded.isEmpty())
        assertEquals(
            context.getString(com.truckerload.R.string.drive_sync_not_linked),
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun backupNow_failsWhenApiKeyMissing() = runBlocking {
        val store = FakeDriveRemoteStore()
        val result = flow(store, apiKey = false, createJson = { "{}" }).backupNow(context)
        assertTrue(result.isFailure)
        assertTrue(store.uploaded.isEmpty())
        assertEquals(
            context.getString(com.truckerload.R.string.drive_sync_key_missing),
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun backupNow_recordsUploadFailure() = runBlocking {
        val store = FakeDriveRemoteStore(
            uploadResult = Result.failure(GoogleDriveApiClient.DriveError.Api("HTTP 503")),
        )
        val result = flow(store, createJson = { """{"loads":[]}""" }).backupNow(context)
        assertTrue(result.isFailure)
        assertEquals(0L, prefs.lastSyncAt)
        assertTrue(prefs.lastSyncError?.contains("HTTP 503") == true)
        assertTrue(prefs.lastSyncErrorAt > 0L)
    }

    @Test
    fun restoreNow_downloadsAndAppliesJson() = runBlocking {
        val backup = BackupTestFixtures.sampleBackup()
        val json = BackupDataCodec.toJson(backup)
        val store = FakeDriveRemoteStore(remoteJson = json)
        var restored: BackupData? = null
        val result = flow(
            store,
            restoreJson = { _, body, allowUnbound ->
                assertEquals(json, body)
                assertFalse(allowUnbound)
                restored = BackupDataCodec.decode(body)
                Result.success(restored!!)
            },
        ).restoreNow(context)
        assertTrue(result.isSuccess)
        assertEquals(backup.loads.size, restored?.loads?.size)
        assertEquals(1, restored?.paychecks?.size)
        assertNull(prefs.lastSyncError)
        assertTrue(prefs.lastSyncAt > 0L)
        assertTrue(result.getOrThrow().contains("1"))
    }

    @Test
    fun pushAutoBackup_uploadsWhenRemoteMissing() = runBlocking {
        val store = FakeDriveRemoteStore(remoteExists = false)
        val json = BackupDataCodec.toJson(BackupTestFixtures.sampleBackup())
        val ok = flow(store, createJson = { json }).pushAutoBackupIfEnabled(context)
        assertTrue(ok)
        assertEquals(listOf(json), store.uploaded)
        assertNull(prefs.lastSyncError)
    }

    @Test
    fun pushAutoBackup_skipsWhenRemoteIsNewer() = runBlocking {
        val store = FakeDriveRemoteStore(remoteExists = true)
        prefs.remoteModifiedAt = 20_000L
        prefs.lastSyncAt = 1_000L
        val ok = flow(store, createJson = { """{"loads":[1]}""" }).pushAutoBackupIfEnabled(context)
        assertTrue(ok)
        assertTrue(store.uploaded.isEmpty())
    }

    @Test
    fun pushAutoBackup_recordsMetadataFailure() = runBlocking {
        val store = FakeDriveRemoteStore(
            refreshResult = Result.failure(GoogleDriveApiClient.DriveError.Api("timeout")),
        )
        val ok = flow(store, createJson = { "{}" }).pushAutoBackupIfEnabled(context)
        assertFalse(ok)
        assertEquals("drive_metadata_unavailable", prefs.lastSyncError)
        assertTrue(store.uploaded.isEmpty())
    }

    private fun flow(
        store: FakeDriveRemoteStore,
        apiKey: Boolean = true,
        linked: Boolean = true,
        createJson: suspend (Context) -> String? = { null },
        restoreJson: suspend (Context, String, Boolean) -> Result<BackupData> = { _, _, _ ->
            Result.failure(IllegalStateException("restore not stubbed"))
        },
    ) = DriveBackupFlow(
        isApiKeyConfigured = { apiKey },
        isLinkedOrGranted = { linked },
        syncLinkedAccount = {},
        createBackupJson = createJson,
        restoreBackupJson = restoreJson,
        clientFactory = { _, _ -> store },
    )

    private class FakeDriveRemoteStore(
        var remoteJson: String? = null,
        var remoteExists: Boolean = remoteJson != null,
        var uploadResult: Result<Unit> = Result.success(Unit),
        var refreshResult: Result<Boolean>? = null,
        val uploaded: MutableList<String> = mutableListOf(),
    ) : DriveRemoteStore {
        override suspend fun uploadBackupJson(json: String): Result<Unit> {
            if (uploadResult.isSuccess) {
                uploaded += json
                remoteJson = json
                remoteExists = true
            }
            return uploadResult
        }

        override suspend fun downloadBackupJson(): Result<String> {
            return remoteJson?.let { Result.success(it) }
                ?: Result.failure(GoogleDriveApiClient.DriveError.Api("No backup on Google Drive yet"))
        }

        override suspend fun refreshRemoteBackup(): Result<Boolean> {
            return refreshResult ?: Result.success(remoteExists)
        }
    }
}
