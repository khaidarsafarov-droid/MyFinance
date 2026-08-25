package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.MiscExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiscExpenseDao {

    @Query("SELECT * FROM misc_expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<MiscExpenseEntity>>

    @Query("SELECT * FROM misc_expenses ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<MiscExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: MiscExpenseEntity): Long

    @Query("DELETE FROM misc_expenses WHERE id = :id")
    suspend fun deleteById(id: Int)
}
