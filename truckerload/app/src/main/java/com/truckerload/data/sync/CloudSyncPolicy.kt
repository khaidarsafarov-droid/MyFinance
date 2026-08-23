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

    /** Int-keyed variant for paycheck / diesel rows. */
    fun orphanLocalIntIds(localIds: Set<Int>, remoteIds: Set<Int>): Set<Int> =
        localIds - remoteIds

    /** True when device has never completed a cloud pull/push for this account. */
    fun needsFullHydration(lastSyncedAt: Long, localEntityCount: Int, remoteEntityCount: Int): Boolean =
        lastSyncedAt <= 0L && localEntityCount == 0 && remoteEntityCount > 0

    /** Incremental pull is useful when we already have a cursor and remote may have newer rows. */
    fun shouldPullIncremental(lastSyncedAt: Long, remoteUpdatedAt: Long): Boolean =
        lastSyncedAt > 0L && remoteUpdatedAt > lastSyncedAt
}
