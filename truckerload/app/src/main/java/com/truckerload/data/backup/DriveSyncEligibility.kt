package com.truckerload.data.backup

import com.truckerload.data.preferences.AccountIds

/**
 * Google Drive backup is independent of Ktor/Supabase cloud sync.
 * Enqueue whenever the user has a real account (not guest [AccountIds.LOCAL_DEV]).
 */
object DriveSyncEligibility {
    fun shouldEnqueuePeriodic(userId: String?): Boolean {
        val id = userId?.trim().orEmpty()
        return id.isNotEmpty() && id != AccountIds.LOCAL_DEV
    }
}
