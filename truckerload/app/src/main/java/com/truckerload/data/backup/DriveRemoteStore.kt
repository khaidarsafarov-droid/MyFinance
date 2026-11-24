package com.truckerload.data.backup

/**
 * Narrow Drive surface used by backup / restore / auto-push.
 * Production uses [GoogleDriveApiClient]; tests supply an in-memory fake.
 */
interface DriveRemoteStore {
    suspend fun uploadBackupJson(json: String): Result<Unit>
    suspend fun downloadBackupJson(): Result<String>
    suspend fun refreshRemoteBackup(): Result<Boolean>
}
