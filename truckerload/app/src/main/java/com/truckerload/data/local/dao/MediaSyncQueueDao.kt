package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.MediaSyncQueueEntity

@Dao
interface MediaSyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MediaSyncQueueEntity)

    @Query("SELECT * FROM media_sync_queue WHERE kind = :kind AND localId = :localId LIMIT 1")
    suspend fun get(kind: String, localId: String): MediaSyncQueueEntity?

    @Query(
        """
        SELECT * FROM media_sync_queue
        WHERE status = 'PENDING'
        ORDER BY createdAt ASC
        LIMIT :limit
        """,
    )
    suspend fun pending(limit: Int): List<MediaSyncQueueEntity>

    @Query(
        """
        UPDATE media_sync_queue SET status = 'PROCESSING'
        WHERE id = :id AND operation = :operation AND updatedAt = :generation AND status = 'PENDING'
        """,
    )
    suspend fun markProcessing(id: String, operation: String, generation: Long): Int

    @Query(
        """
        UPDATE media_sync_queue SET remoteMediaId = :remoteMediaId
        WHERE id = :id AND operation = :operation AND updatedAt = :generation
        """,
    )
    suspend fun setRemoteMediaId(
        id: String,
        operation: String,
        generation: Long,
        remoteMediaId: String,
    ): Int

    @Query(
        """
        UPDATE media_sync_queue SET remoteMediaId = :remoteMediaId
        WHERE kind = :kind AND localId = :localId AND operation = 'DELETE'
        """,
    )
    suspend fun attachRemoteIdToDelete(kind: String, localId: String, remoteMediaId: String): Int

    @Query(
        """
        UPDATE media_sync_queue
        SET status = :status, attempts = :attempts, lastError = :lastError
        WHERE id = :id AND operation = :operation AND updatedAt = :generation
        """,
    )
    suspend fun updateAttempt(
        id: String,
        operation: String,
        generation: Long,
        status: String,
        attempts: Int,
        lastError: String?,
    ): Int

    @Query(
        """
        UPDATE media_sync_queue SET status = 'PENDING'
        WHERE status = 'PROCESSING' AND updatedAt < :olderThan
        """,
    )
    suspend fun resetStuck(olderThan: Long)

    @Query(
        """
        DELETE FROM media_sync_queue
        WHERE id = :id AND operation = :operation AND updatedAt = :generation
        """,
    )
    suspend fun deleteIfCurrent(id: String, operation: String, generation: Long): Int

    @Query("DELETE FROM media_sync_queue WHERE kind = :kind AND localId = :localId")
    suspend fun deleteForLocal(kind: String, localId: String)

    @Query("SELECT COUNT(*) FROM media_sync_queue WHERE status = :status")
    suspend fun count(status: String): Int
}
