package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.DeletedLoadLedger

/** Finishes deletes that were interrupted by process death or leaving Home. */
object LoadPendingDeleteApplier {
    suspend fun apply(repository: LoadRepository) {
        val ctx = AppDatabase.applicationContext() ?: return
        val ids = DeletedLoadLedger.pendingHardDeleteIds(ctx) +
            DeletedLoadLedger.blockedLoadIds(ctx).filter { id ->
                runCatching { repository.getLoadById(id) }.getOrNull() != null
            }
        ids.forEach { id ->
            if (runCatching { repository.getLoadById(id) }.getOrNull() != null) {
                repository.deleteLoad(id)
            } else {
                DeletedLoadLedger.markDeleted(ctx, id, null)
            }
        }
    }
}
