package com.truckerload.sync

import com.truckerload.data.local.entities.SyncOutboxEntity

data class OutboxStatusUpdate(
    val status: String,
    val attempts: Int,
    val lastError: String?,
)

object OutboundSyncPolicy {
    fun afterSnapshotUpload(
        currentAttempts: Int,
        uploadAcknowledged: Boolean,
    ): OutboxStatusUpdate =
        if (uploadAcknowledged) {
            OutboxStatusUpdate(
                status = SyncOutboxEntity.STATUS_SYNCED,
                attempts = currentAttempts + 1,
                lastError = null,
            )
        } else {
            OutboxStatusUpdate(
                status = SyncOutboxEntity.STATUS_PENDING,
                attempts = currentAttempts + 1,
                lastError = "snapshot_upload_failed",
            )
        }
}
