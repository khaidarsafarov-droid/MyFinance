package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.truckerload.data.local.entities.LoadHistory

@Dao
interface LoadHistoryDao {
    @Insert
    suspend fun insert(history: LoadHistory)

    @Query("SELECT * FROM load_history WHERE loadId = :loadId ORDER BY timestamp DESC")
    suspend fun getHistory(loadId: String): List<LoadHistory>

    @Query("DELETE FROM load_history WHERE loadId = :loadId")
    suspend fun deleteByLoadId(loadId: String)

    @Query("DELETE FROM load_history")
    suspend fun deleteAll()
}
