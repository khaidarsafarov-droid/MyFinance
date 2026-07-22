package com.truckerload.data.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncPolicyExtraTest {
    @Test
    fun warnsWhenLocalDirtyAndRemoteNewer() {
        assertTrue(
            DriveSyncPolicy.shouldWarnBeforeRestore(
                localChangedAfterLastSync = true,
                remoteModifiedAt = 10_000L,
                lastSyncAt = 1_000L,
            )
        )
    }

    @Test
    fun noWarnWhenLocalClean() {
        assertFalse(
            DriveSyncPolicy.shouldWarnBeforeRestore(
                localChangedAfterLastSync = false,
                remoteModifiedAt = 10_000L,
                lastSyncAt = 1_000L,
            )
        )
    }
}
