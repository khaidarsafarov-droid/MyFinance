package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.truckerload.data.local.entities.LoadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadDao {

    /** CDC: возвращает множество Trip ID, уже присутствующих в БД. Для фильтрации входящих данных в памяти. */
    @Query("SELECT tripId FROM loads WHERE tripId IN (:tripIds)")
    suspend fun getExistingTripIds(tripIds: List<String>): List<String>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    fun getAllLoads(): Flow<List<LoadEntity>>

    @Query("SELECT * FROM loads WHERE id = :loadId")
    suspend fun getLoadById(loadId: String): LoadEntity?

    @Query("SELECT * FROM loads WHERE date LIKE :monthPrefix ORDER BY parsedAt DESC")
    fun getLoadsByMonth(monthPrefix: String): Flow<List<LoadEntity>>

    @Query("SELECT * FROM loads WHERE tripId LIKE '%' || :query || '%' OR pointA LIKE '%' || :query || '%' OR pointB LIKE '%' || :query || '%' OR date LIKE '%' || :query || '%' ORDER BY parsedAt DESC")
    fun searchLoads(query: String): Flow<List<LoadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(load: LoadEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(loads: List<LoadEntity>)

    @Query("SELECT * FROM loads WHERE weekNumber = :weekNumber AND year = :year ORDER BY parsedAt DESC")
    fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<LoadEntity>>

    @Query("UPDATE loads SET date = :loadDate, totalRate = :totalRate, totalMiles = :totalMiles, pointA = :pointA, pointB = :pointB, weekNumber = :weekNumber, year = :year, updatedAt = :updatedAt WHERE id = :loadId")
    suspend fun update(loadId: String, loadDate: String, totalRate: Double, totalMiles: Double, pointA: String, pointB: String, weekNumber: Int, year: Int, updatedAt: Long)

    /** Точный поиск по load_date (YYYY-MM-DD). */
    @Query("SELECT * FROM loads WHERE date = :loadDate ORDER BY parsedAt DESC")
    fun getLoadsByDate(loadDate: String): Flow<List<LoadEntity>>

    /** Поиск по диапазону дат (включительно). */
    @Query("SELECT * FROM loads WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, parsedAt DESC")
    fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<LoadEntity>>

    @Query("DELETE FROM loads WHERE id = :loadId")
    suspend fun deleteById(loadId: String)

    @Query("DELETE FROM loads")
    suspend fun deleteAll()
}
