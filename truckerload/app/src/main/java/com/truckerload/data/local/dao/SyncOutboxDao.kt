package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncOutboxEntity)

    @Query(
        """
        SELECT * FROM sync_outbox
        WHERE status = :status
        ORDER BY createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun listByStatus(status: String, limit: Int = 50): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Query(
        """
        UPDATE sync_outbox
        SET status = :status, attempts = :attempts, lastError = :lastError, updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        attempts: Int,
        lastError: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("DELETE FROM sync_outbox WHERE status = :status AND updatedAt < :olderThan")
    suspend fun deleteOlderThan(status: String, olderThan: Long)
}
