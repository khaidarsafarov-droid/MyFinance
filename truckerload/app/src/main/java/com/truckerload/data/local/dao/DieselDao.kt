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

    @Query("SELECT * FROM diesel WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): DieselEntity?

    /** Reporting-period rows; pass empty [minDate] / [maxDate] for an open bound. */
    @Query(
        """
        SELECT * FROM diesel
        WHERE (:minDate = '' OR weekEndDate >= :minDate)
          AND (:maxDate = '' OR weekEndDate <= :maxDate)
        """,
    )
    suspend fun getDieselSince(minDate: String, maxDate: String): List<DieselEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(diesel: DieselEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dieselList: List<DieselEntity>)

    @Query("DELETE FROM diesel WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM diesel")
    suspend fun deleteAll()
}
