package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.StopEntity

@Dao
interface StopDao {

    @Query("SELECT * FROM stops WHERE loadId = :loadId ORDER BY stopNumber ASC")
    suspend fun getStopsByLoadId(loadId: String): List<StopEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)

    @Query("DELETE FROM stops WHERE loadId = :loadId")
    suspend fun deleteByLoadId(loadId: String)
}
