package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos")
    suspend fun getAllPhotosOnce(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE loadId = :loadId ORDER BY timestamp DESC")
    fun getPhotosByLoadId(loadId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE loadId = :loadId")
    suspend fun getPhotosByLoadIdOnce(loadId: String): List<PhotoEntity>

    @Query(
        """
        SELECT * FROM photos
        WHERE (:loadId IS NULL OR loadId = :loadId)
          AND (:dayStartMillis IS NULL OR timestamp >= :dayStartMillis)
          AND (:dayEndMillis IS NULL OR timestamp <= :dayEndMillis)
        ORDER BY timestamp DESC
        """,
    )
    fun getPhotosFiltered(
        loadId: String?,
        dayStartMillis: Long?,
        dayEndMillis: Long?,
    ): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE loadId IS NULL OR loadId = '' ORDER BY timestamp DESC")
    fun getUnlinkedPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET loadId = :loadId WHERE id = :id")
    suspend fun updateLoadId(id: String, loadId: String?)

    @Query(
        """
        UPDATE photos
        SET cloudMediaId = :remoteMediaId, cloudSyncStatus = :status, cloudUpdatedAt = :cloudUpdatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateCloudState(
        id: String,
        remoteMediaId: String?,
        status: String,
        cloudUpdatedAt: Long,
    ): Int

    @Query("UPDATE photos SET cloudSyncStatus = :status WHERE id = :id")
    suspend fun updateCloudStatus(id: String, status: String): Int

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM photos WHERE loadId = :loadId")
    suspend fun deleteByLoadId(loadId: String)

    @Query("DELETE FROM photos")
    suspend fun deleteAll()
}
