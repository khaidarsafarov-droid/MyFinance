package com.truckerload.data.backup

/**
 * Google Drive App Folder backup is independent of any server.
 * Any saved local session (including [com.truckerload.data.preferences.AccountIds.LOCAL_DEV])
 * can opt in from Settings.
 */
object DriveSyncEligibility {
    fun shouldEnqueuePeriodic(userId: String?): Boolean = !userId.isNullOrBlank()
}
