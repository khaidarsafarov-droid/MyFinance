package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.PenaltyEntity

@Dao
interface PenaltyDao {

    @Query("SELECT * FROM penalties WHERE loadId = :loadId")
    suspend fun getPenaltiesByLoadId(loadId: String): List<PenaltyEntity>

    @Query("SELECT * FROM penalties WHERE loadId IN (:loadIds)")
    suspend fun getPenaltiesByLoadIds(loadIds: List<String>): List<PenaltyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(penalties: List<PenaltyEntity>)

    @Query("DELETE FROM penalties WHERE loadId = :loadId")
    suspend fun deleteByLoadId(loadId: String)
}
