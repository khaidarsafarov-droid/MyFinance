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

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE loadId = :loadId ORDER BY timestamp DESC")
    fun getPhotosByLoadId(loadId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE loadId IS NULL OR loadId = '' ORDER BY timestamp DESC")
    fun getUnlinkedPhotos(): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET loadId = :loadId WHERE id = :id")
    suspend fun updateLoadId(id: String, loadId: String?)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: String)
}
