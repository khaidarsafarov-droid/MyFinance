package com.truckerload.domain.model

/**
 * LWW version for diesel/paycheck rows that store [addedAt] instead of updatedAt.
 * Edits must bump this or a pull with the same timestamp will keep the stale amount.
 */
object JournalSyncClock {
    fun bump(previousAddedAt: Long, now: Long = System.currentTimeMillis()): Long =
        maxOf(now, previousAddedAt + 1L)
}
