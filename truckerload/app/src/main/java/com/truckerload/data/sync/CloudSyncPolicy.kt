package com.truckerload.data.sync

/**
 * Conflict resolution for account-based cloud sync (Last Write Wins).
 * Pure — no Android / Room dependencies.
 */
object CloudSyncPolicy {
    const val DEFAULT_SKEW_MS = 0L

    /**
     * Returns true when [remoteUpdatedAt] should replace the local record.
     * Equal timestamps keep local (stable; avoids thrashing).
     */
    fun remoteWins(
        localUpdatedAt: Long?,
        remoteUpdatedAt: Long,
        skewMs: Long = DEFAULT_SKEW_MS,
    ): Boolean {
        if (localUpdatedAt == null || localUpdatedAt <= 0L) return true
        return remoteUpdatedAt > localUpdatedAt + skewMs
    }

    /**
     * Merge two maps keyed by stable entity id using LWW on [updatedAt].
     * Entries only in [local] are kept; only in [remote] are added;
     * both → winner by [remoteWins].
     *
     * **Do not use for full-snapshot push.** Re-adding remote-only ids resurrects
     * locally deleted entities. Prefer [localSnapshotForPush] for outbound publish.
     */
    fun <T> mergeById(
        local: Map<String, T>,
        remote: Map<String, T>,
        updatedAt: (T) -> Long,
    ): Map<String, T> {
        val out = LinkedHashMap<String, T>(local.size + remote.size)
        out.putAll(local)
        for ((id, remoteItem) in remote) {
            val localItem = out[id]
            if (localItem == null || remoteWins(updatedAt(localItem), updatedAt(remoteItem))) {
                out[id] = remoteItem
            }
        }
        return out
    }

    /**
     * Outbound full snapshot: publish the local entity set as authoritative.
     * After an incremental pull, Room already holds the merged truth; merging the
     * previous remote blob again would revive rows the user deleted locally.
     */
    fun <T> localSnapshotForPush(local: Map<String, T>): Map<String, T> = local

    /**
     * Local ids present in Room but absent from a full remote snapshot — orphans to delete
     * so cross-device deletions propagate on pull.
     */
    fun orphanLocalIds(localIds: Set<String>, remoteIds: Set<String>): Set<String> =
        localIds - remoteIds

    /**
     * Safe orphan set for incremental pull: never delete local-only rows that were created or
     * edited after [lastSyncedAt] (not yet published to cloud). Prevents pull-before-push data loss.
     */
    fun orphanLocalIdsForPull(
        localIds: Set<String>,
        remoteIds: Set<String>,
        localUpdatedAt: (String) -> Long,
        lastSyncedAt: Long,
    ): Set<String> {
        // FIX: first sync / never acked — remote snapshot may omit unpushed local rows
        if (lastSyncedAt <= 0L) return emptySet()
        return (localIds - remoteIds).filter { id ->
            val updated = localUpdatedAt(id)
            updated > 0L && updated <= lastSyncedAt
        }.toSet()
    }

    /** Int-keyed variant for paycheck / diesel rows. */
    fun orphanLocalIntIds(localIds: Set<Int>, remoteIds: Set<Int>): Set<Int> =
        localIds - remoteIds

    /**
     * Safe orphan set for diesel/paycheck incremental pull (version field = [addedAt]).
     */
    fun orphanLocalIntIdsForPull(
        localIds: Set<Int>,
        remoteIds: Set<Int>,
        localAddedAt: (Int) -> Long,
        lastSyncedAt: Long,
    ): Set<Int> {
        if (lastSyncedAt <= 0L) return emptySet()
        return (localIds - remoteIds).filter { id ->
            val added = localAddedAt(id)
            added > 0L && added <= lastSyncedAt
        }.toSet()
    }

    /**
     * Ids from [remoteById] that should be upserted on incremental pull:
     * new remote rows, or existing rows where remote [updatedAt] wins LWW.
     * Diesel/paycheck use [addedAt] as the version field.
     */
    fun <T> remoteIntIdsToApplyOnPull(
        localById: Map<Int, T>,
        remoteById: Map<Int, T>,
        updatedAt: (T) -> Long,
    ): Set<Int> {
        val out = LinkedHashSet<Int>(remoteById.size)
        for ((id, remoteItem) in remoteById) {
            val localItem = localById[id]
            val localTs = localItem?.let(updatedAt)
            if (localItem == null || remoteWins(localTs, updatedAt(remoteItem))) {
                out.add(id)
            }
        }
        return out
    }

    /** True when device has never completed a cloud pull/push for this account. */
    fun needsFullHydration(lastSyncedAt: Long, localEntityCount: Int, remoteEntityCount: Int): Boolean =
        lastSyncedAt <= 0L && localEntityCount == 0 && remoteEntityCount > 0

    /**
     * First cloud login on a device that already has a local journal (restore / offline use).
     * Requires LWW merge from remote without deleting unpushed local rows.
     */
    fun needsInitialMerge(lastSyncedAt: Long, localEntityCount: Int, remoteEntityCount: Int): Boolean =
        lastSyncedAt <= 0L && localEntityCount > 0 && remoteEntityCount > 0

    /** Incremental pull is useful when we already have a cursor and remote may have newer rows. */
    fun shouldPullIncremental(lastSyncedAt: Long, remoteUpdatedAt: Long): Boolean =
        lastSyncedAt > 0L && remoteUpdatedAt > lastSyncedAt
}
