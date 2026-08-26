package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    /** Reporting-period rows; pass empty [minDate] / [maxDate] for an open bound. */
    @Query(
        """
        SELECT * FROM paychecks
        WHERE (:minDate = '' OR weekEndDate >= :minDate)
          AND (:maxDate = '' OR weekEndDate <= :maxDate)
        """,
    )
    suspend fun getPaychecksSince(minDate: String, maxDate: String): List<PaycheckEntity>

    @Query("SELECT * FROM paychecks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): PaycheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(paycheck: PaycheckEntity)

    @Update
    suspend fun update(paycheck: PaycheckEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paychecks: List<PaycheckEntity>)

    @Query("DELETE FROM paychecks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM paychecks")
    suspend fun deleteAll()
}
