package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.CrowdRateEntity

@Dao
interface CrowdRateDao {
    @Query(
        """
        SELECT * FROM crowd_rates
        WHERE reportedAtMillis >= :sinceMillis
        ORDER BY reportedAtMillis DESC
        """,
    )
    suspend fun listSince(sinceMillis: Long): List<CrowdRateEntity>

    @Query(
        """
        SELECT * FROM crowd_rates
        WHERE source = :source AND reportedAtMillis >= :sinceMillis
        ORDER BY reportedAtMillis DESC
        """,
    )
    suspend fun listBySourceSince(source: String, sinceMillis: Long): List<CrowdRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<CrowdRateEntity>)

    @Query("DELETE FROM crowd_rates WHERE reportedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("SELECT COUNT(*) FROM crowd_rates WHERE source = :source")
    suspend fun countBySource(source: String): Int
}
