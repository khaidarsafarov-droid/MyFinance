package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.ScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity)

    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans")
    suspend fun getAllScansOnce(): List<ScanEntity>

    @Query("SELECT * FROM scans WHERE loadId = :loadId ORDER BY timestamp DESC")
    fun getScansByLoadId(loadId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE loadId = :loadId")
    suspend fun getScansByLoadIdOnce(loadId: String): List<ScanEntity>

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM scans WHERE loadId = :loadId")
    suspend fun deleteByLoadId(loadId: String)

    @Query("DELETE FROM scans")
    suspend fun deleteAll()
}
