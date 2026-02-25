package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.DieselEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DieselDao {

    @Query("SELECT * FROM diesel ORDER BY addedAt DESC")
    fun getAllDiesel(): Flow<List<DieselEntity>>

    @Query("SELECT * FROM diesel WHERE weekNumber = :weekNumber AND year = :year")
    fun getDieselForWeek(weekNumber: Int, year: Int): Flow<List<DieselEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(diesel: DieselEntity)

    @Query("DELETE FROM diesel WHERE id = :id")
    suspend fun deleteById(id: Int)
}
