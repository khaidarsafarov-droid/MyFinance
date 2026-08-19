package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.CommunityProfileEntity
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.local.entities.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun get(id: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    fun watch(id: String): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserAccountEntity)

    @Query("DELETE FROM user_accounts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM user_accounts")
    suspend fun deleteAll()
}

@Dao
interface DriverProfessionalDao {
    @Query("SELECT * FROM driver_professional_profiles WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): DriverProfessionalEntity?

    @Query("SELECT * FROM driver_professional_profiles WHERE userId = :userId LIMIT 1")
    fun watch(userId: String): Flow<DriverProfessionalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DriverProfessionalEntity)

    @Query("DELETE FROM driver_professional_profiles WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM driver_professional_profiles")
    suspend fun deleteAll()
}

@Dao
interface CommunityProfileDao {
    @Query("SELECT * FROM community_profiles WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): CommunityProfileEntity?

    @Query("SELECT * FROM community_profiles WHERE userId = :userId LIMIT 1")
    fun watch(userId: String): Flow<CommunityProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommunityProfileEntity)

    @Query("DELETE FROM community_profiles WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM community_profiles")
    suspend fun deleteAll()
}
