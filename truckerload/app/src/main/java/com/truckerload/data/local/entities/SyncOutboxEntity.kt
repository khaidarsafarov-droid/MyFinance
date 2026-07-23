package com.truckerload.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hybrid outbound queue: local mutations wait here until the bot/server ack.
 * WorkManager drains pending rows when connectivity returns.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["status", "createdAt"]),
        Index(value = ["entityType", "entityId"]),
        Index(value = ["status", "updatedAt"]),
    ],
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val op: String,
    val payloadJson: String,
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_FAILED = "FAILED"

        const val TYPE_LOAD = "LOAD"
        const val TYPE_DIESEL = "DIESEL"
        const val TYPE_PROFILE = "PROFILE"
        const val TYPE_STATUS = "STATUS"

        const val OP_UPSERT = "UPSERT"
        const val OP_DELETE = "DELETE"
    }
}
