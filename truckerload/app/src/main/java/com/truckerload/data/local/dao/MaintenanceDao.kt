package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truckerload.data.local.entities.MaintenanceArchiveEntity
import com.truckerload.data.local.entities.MaintenanceTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {

    @Query("SELECT * FROM maintenance_tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun watchTasks(): Flow<List<MaintenanceTaskEntity>>

    @Query("SELECT * FROM maintenance_tasks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveTasksOnce(): List<MaintenanceTaskEntity>

    @Query("SELECT * FROM maintenance_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): MaintenanceTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: MaintenanceTaskEntity): Long

    @Update
    suspend fun updateTask(task: MaintenanceTaskEntity)

    @Query("DELETE FROM maintenance_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM maintenance_archive ORDER BY serviceDate DESC, createdAt DESC")
    fun watchArchive(): Flow<List<MaintenanceArchiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(entry: MaintenanceArchiveEntity): Long

    @Query("SELECT * FROM maintenance_archive WHERE id = :id LIMIT 1")
    suspend fun getArchiveById(id: Long): MaintenanceArchiveEntity?

    @Query("DELETE FROM maintenance_archive WHERE id = :id")
    suspend fun deleteArchive(id: Long)
}
