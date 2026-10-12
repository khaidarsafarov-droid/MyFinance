package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.DeletedLoadLedger

/** Finishes deletes that were interrupted by process death or leaving Home. */
object LoadPendingDeleteApplier {
    suspend fun apply(repository: LoadRepository) {
        val ctx = AppDatabase.applicationContext() ?: return
        // Only finish deletes that were interrupted before the undo window closed.
        // A blocked id is a tombstone for future ingest, not a command to delete a row:
        // explicit backup restore may legitimately bring that id back.
        val ids = DeletedLoadLedger.pendingHardDeleteIds(ctx)
        ids.forEach { id ->
            if (runCatching { repository.getLoadById(id) }.getOrNull() != null) {
                repository.deleteLoad(id)
            } else {
                DeletedLoadLedger.markDeleted(ctx, id, null)
            }
        }
    }
}
