package com.truckerload.data.sync.cloud

import com.truckerload.data.sync.CloudSyncPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last-write-wins conflict rules for account cloud sync.
 * Delegates to [CloudSyncPolicy] so Room merge and snapshot merge stay identical.
 */
@Singleton
class SyncConflictResolver @Inject constructor() {
    fun remoteWins(localUpdatedAt: Long?, remoteUpdatedAt: Long, skewMs: Long = 0L): Boolean =
        CloudSyncPolicy.remoteWins(localUpdatedAt, remoteUpdatedAt, skewMs)

    fun <T> mergeById(
        local: Map<String, T>,
        remote: Map<String, T>,
        updatedAt: (T) -> Long,
    ): Map<String, T> = CloudSyncPolicy.mergeById(local, remote, updatedAt)

    fun needsFullHydration(
        lastSyncedAt: Long,
        localEntityCount: Int,
        remoteEntityCount: Int,
    ): Boolean = CloudSyncPolicy.needsFullHydration(lastSyncedAt, localEntityCount, remoteEntityCount)

    fun shouldPullIncremental(lastSyncedAt: Long, remoteUpdatedAt: Long): Boolean =
        CloudSyncPolicy.shouldPullIncremental(lastSyncedAt, remoteUpdatedAt)
}
