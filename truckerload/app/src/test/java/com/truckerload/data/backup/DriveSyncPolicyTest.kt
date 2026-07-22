package com.truckerload.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncPolicyTest {

    @Test
    fun backoffDelay_growsWithAttempt() {
        assertEquals(400L, DriveSyncPolicy.backoffDelayMs(0))
        assertEquals(800L, DriveSyncPolicy.backoffDelayMs(1))
        assertEquals(1200L, DriveSyncPolicy.backoffDelayMs(2))
    }

    @Test
    fun isRetryable_timeoutsAnd5xx() {
        assertTrue(DriveSyncPolicy.isRetryableFailure("timeout"))
        assertTrue(DriveSyncPolicy.isRetryableFailure("HTTP 503"))
        assertTrue(DriveSyncPolicy.isRetryableFailure("429 Too Many Requests"))
        assertFalse(DriveSyncPolicy.isRetryableFailure("Not signed in to Google Drive"))
        assertFalse(DriveSyncPolicy.isRetryableFailure("401 unauthorized"))
    }

    @Test
    fun shouldWarnBeforeRestore_whenLocalDirtyAndRemoteNewer() {
        assertTrue(
            DriveSyncPolicy.shouldWarnBeforeRestore(
                localChangedAfterLastSync = true,
                remoteModifiedAt = 2_000L,
                lastSyncAt = 1_000L,
            )
        )
        assertFalse(
            DriveSyncPolicy.shouldWarnBeforeRestore(
                localChangedAfterLastSync = false,
                remoteModifiedAt = 2_000L,
                lastSyncAt = 1_000L,
            )
        )
        assertFalse(
            DriveSyncPolicy.shouldWarnBeforeRestore(
                localChangedAfterLastSync = true,
                remoteModifiedAt = 1_000L,
                lastSyncAt = 2_000L,
            )
        )
    }

    @Test
    fun remoteIsNewer_respectsSkew() {
        assertTrue(DriveSyncPolicy.remoteIsNewer(10_000L, 5_000L))
        assertFalse(DriveSyncPolicy.remoteIsNewer(5_000L, 5_000L))
        assertFalse(DriveSyncPolicy.remoteIsNewer(5_500L, 5_000L, skewMs = 2_000L))
    }
}
