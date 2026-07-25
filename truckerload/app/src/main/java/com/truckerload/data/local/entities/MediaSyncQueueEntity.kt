package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable account-local media work. The unique kind/client id row lets a delete
 * supersede an in-flight upload without losing a discovered remote media id.
 */
@Entity(
    tableName = "media_sync_queue",
    indices = [
        Index(value = ["kind", "localId"], unique = true),
        Index(value = ["status", "createdAt"]),
        Index(value = ["status", "updatedAt"]),
    ],
)
data class MediaSyncQueueEntity(
    @PrimaryKey val id: String,
    val localId: String,
    val kind: String,
    val operation: String,
    val remoteMediaId: String? = null,
    val filePath: String? = null,
    val metadataJson: String = "{}",
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val KIND_PHOTO = "PHOTO"
        const val KIND_SCAN = "SCAN"
        const val OP_UPSERT = "UPSERT"
        const val OP_DELETE = "DELETE"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_FAILED = "FAILED"
    }
}
