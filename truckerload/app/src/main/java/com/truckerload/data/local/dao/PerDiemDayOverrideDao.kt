package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.PerDiemDayOverrideEntity

@Dao
interface PerDiemDayOverrideDao {

    @Query("SELECT * FROM per_diem_day_overrides WHERE date LIKE :yearPattern")
    suspend fun getForYear(yearPattern: String): List<PerDiemDayOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: PerDiemDayOverrideEntity)

    @Query("DELETE FROM per_diem_day_overrides WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
