package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.WeekYieldAgg
import com.truckerload.data.local.entities.analytics.AnalyticsTotalsAgg
import com.truckerload.data.local.entities.analytics.DailyGrossAgg
import com.truckerload.data.local.entities.analytics.WeeklyRevenueAgg
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadDao {

    /** CDC: возвращает множество Trip ID, уже присутствующих в БД. Для фильтрации входящих данных в памяти. */
    @Query("SELECT tripId FROM loads WHERE tripId IN (:tripIds)")
    suspend fun getExistingTripIds(tripIds: List<String>): List<String>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    suspend fun getAllLoadsOnce(): List<LoadEntity>

    @Query("SELECT * FROM loads WHERE (:minDate = '' OR date >= :minDate) ORDER BY parsedAt DESC")
    suspend fun getLoadsSince(minDate: String): List<LoadEntity>

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

    @Query(
        """
        UPDATE loads SET
            date = :loadDate,
            totalRate = :totalRate,
            totalMiles = :totalMiles,
            pointA = :pointA,
            pointB = :pointB,
            weekNumber = :weekNumber,
            year = :year,
            updatedAt = :updatedAt,
            firstPuMillis = :firstPuMillis,
            lastDelMillis = :lastDelMillis,
            route = :route,
            firstPuCityState = :firstPuCityState,
            lastDelCityState = :lastDelCityState,
            durationDays = :durationDays,
            pace = :pace,
            stopCount = :stopCount
        WHERE id = :loadId
        """
    )
    suspend fun update(
        loadId: String,
        loadDate: String,
        totalRate: Double,
        totalMiles: Double,
        pointA: String,
        pointB: String,
        weekNumber: Int,
        year: Int,
        updatedAt: Long,
        firstPuMillis: Long?,
        lastDelMillis: Long?,
        route: String,
        firstPuCityState: String,
        lastDelCityState: String,
        durationDays: Double,
        pace: Double,
        stopCount: Int,
    )

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

    /**
     * Темп ($/день): SUM(totalRate) / SUM(active days per load).
     * Active days = ceil((lastDel − firstPu) / 86_400_000), min 1; без PU/DEL → 1 день.
     */
    @Query(
        """
        SELECT CASE
            WHEN agg.totalActiveDays > 0 THEN agg.totalGross / agg.totalActiveDays
            ELSE 0.0
        END
        FROM (
            SELECT
                COALESCE(SUM(totalRate), 0.0) AS totalGross,
                COALESCE(SUM(
                    CASE
                        WHEN durationDays > 0 THEN durationDays
                        WHEN firstPuMillis IS NOT NULL AND lastDelMillis IS NOT NULL
                             AND lastDelMillis > firstPuMillis
                        THEN MAX(1.0, CAST((lastDelMillis - firstPuMillis + 86399999) / 86400000 AS REAL))
                        ELSE 1.0
                    END
                ), 0.0) AS totalActiveDays
            FROM loads
            WHERE weekNumber = :weekNumber AND year = :year
        ) AS agg
        """
    )
    fun watchActualDailyYield(weekNumber: Int, year: Int): Flow<Double>

    @Query(
        """
        SELECT
            COALESCE(SUM(totalRate), 0.0) AS totalGross,
            COALESCE(SUM(
                CASE
                    WHEN durationDays > 0 THEN durationDays
                    WHEN firstPuMillis IS NOT NULL AND lastDelMillis IS NOT NULL
                         AND lastDelMillis > firstPuMillis
                    THEN MAX(1.0, CAST((lastDelMillis - firstPuMillis + 86399999) / 86400000 AS REAL))
                    ELSE 1.0
                END
            ), 0.0) AS totalActiveDays
        FROM loads
        WHERE weekNumber = :weekNumber AND year = :year
        """
    )
    fun watchWeekYieldAgg(weekNumber: Int, year: Int): Flow<WeekYieldAgg>

    @Query(
        """
        SELECT
            weekNumber,
            year,
            COALESCE(SUM(totalRate), 0.0) AS gross,
            COALESCE(SUM(totalMiles), 0.0) AS miles,
            COUNT(*) AS loadCount
        FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
        GROUP BY year, weekNumber
        ORDER BY year ASC, weekNumber ASC
        """
    )
    suspend fun getWeeklyRevenue(minDate: String): List<WeeklyRevenueAgg>

    @Query("SELECT * FROM loads WHERE weekNumber = :weekNumber AND year = :year ORDER BY parsedAt DESC")
    suspend fun getLoadsByWeekOnce(weekNumber: Int, year: Int): List<LoadEntity>

    @Query(
        """
        SELECT
            CAST(strftime('%w', date) AS INTEGER) AS dayOfWeek,
            COALESCE(SUM(totalRate), 0.0) AS gross,
            COUNT(*) AS loadCount
        FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
        GROUP BY dayOfWeek
        """
    )
    suspend fun getDailyDistribution(minDate: String): List<DailyGrossAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalRate), 0.0) AS gross,
            COALESCE(SUM(totalMiles), 0.0) AS miles
        FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
        """
    )
    suspend fun getAnalyticsTotals(minDate: String): AnalyticsTotalsAgg
}
