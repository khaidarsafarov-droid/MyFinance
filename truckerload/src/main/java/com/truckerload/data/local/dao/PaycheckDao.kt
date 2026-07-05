package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.PaycheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaycheckDao {

    @Query("SELECT * FROM paychecks ORDER BY addedAt DESC")
    fun getAllPaychecks(): Flow<List<PaycheckEntity>>

    @Query("SELECT * FROM paychecks WHERE weekNumber = :weekNumber AND year = :year LIMIT 1")
    suspend fun getPaycheckForWeek(weekNumber: Int, year: Int): PaycheckEntity?

    @Query("SELECT * FROM paychecks WHERE weekNumber = :weekNumber AND year = :year")
    fun getPaychecksForWeek(weekNumber: Int, year: Int): Flow<List<PaycheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(paycheck: PaycheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paychecks: List<PaycheckEntity>)

    @Query("DELETE FROM paychecks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM paychecks")
    suspend fun deleteAll()
}
