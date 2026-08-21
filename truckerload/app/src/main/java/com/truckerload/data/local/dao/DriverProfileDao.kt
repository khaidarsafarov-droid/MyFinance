package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.DriverProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverProfileDao {
    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    fun watchProfile(id: String = DriverProfileEntity.LOCAL_USER_ID): Flow<DriverProfileEntity?>

    @Query("SELECT * FROM driver_profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String = DriverProfileEntity.LOCAL_USER_ID): DriverProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: DriverProfileEntity)
}
