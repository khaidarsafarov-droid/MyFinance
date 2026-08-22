package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.local.entities.WeekYieldAgg
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.data.local.entities.analytics.AnalyticsTotalsAgg
import com.truckerload.data.local.entities.analytics.DailyGrossAgg
import com.truckerload.data.local.entities.analytics.WeeklyRevenueAgg
import kotlinx.coroutines.flow.Flow

@Dao
interface LoadDao {

    /** CDC: возвращает множество Trip ID, уже присутствующих в БД. Для фильтрации входящих данных в памяти. */
    @Query("SELECT tripId FROM loads WHERE tripId COLLATE NOCASE IN (:tripIds)")
    suspend fun getExistingTripIds(tripIds: List<String>): List<String>

    @Query("SELECT * FROM loads WHERE tripId = :tripId COLLATE NOCASE LIMIT 1")
    suspend fun getByTripId(tripId: String): LoadEntity?

    @Query(
        """
        SELECT * FROM loads
        WHERE firstPuCityState = :origin
          AND lastDelCityState = :destination
          AND date = :date
        LIMIT 1
        """
    )
    suspend fun getByRouteAndDate(
        origin: String,
        destination: String,
        date: String,
    ): LoadEntity?

    @Query("SELECT * FROM loads WHERE date = :loadDate")
    suspend fun getLoadsByDateOnce(loadDate: String): List<LoadEntity>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    suspend fun getAllLoadsOnce(): List<LoadEntity>

    /**
     * Candidates for [ParseUtils.sanitizeLoadedMiles]: absurd miles with crushed RPM.
     * Matches the Kotlin guard (miles ≥ 10_000 and rate/miles < 0.5).
     * (No separate drivenMiles column — filter uses totalMiles/totalRate.)
     */
    @Query(
        """
        SELECT * FROM loads
        WHERE totalMiles >= 10000.0
          AND totalRate > 0.0
          AND (totalRate / totalMiles) < 0.5
        """,
    )
    suspend fun getLoadsWithSuspectInflatedMiles(): List<LoadEntity>

    @Query("SELECT * FROM loads ORDER BY updatedAt DESC, parsedAt DESC LIMIT :limit")
    suspend fun getLoadsForLinking(limit: Int): List<LoadEntity>

    @Query("SELECT * FROM loads WHERE (:minDate = '' OR date >= :minDate) ORDER BY parsedAt DESC")
    suspend fun getLoadsSince(minDate: String): List<LoadEntity>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    fun getAllLoads(): Flow<List<LoadEntity>>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    fun pagingAllLoads(): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE weekNumber = :weekNumber AND year = :year ORDER BY parsedAt DESC")
    fun pagingLoadsByWeek(weekNumber: Int, year: Int): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, parsedAt DESC")
    fun pagingLoadsByDateRange(
        startDate: String,
        endDate: String,
    ): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE date = :loadDate ORDER BY parsedAt DESC")
    fun pagingLoadsByDate(loadDate: String): androidx.paging.PagingSource<Int, LoadEntity>

    @Query(
        """
        SELECT * FROM loads
        WHERE tripId LIKE '%' || :query || '%'
           OR pointA LIKE '%' || :query || '%'
           OR pointB LIKE '%' || :query || '%'
           OR date LIKE '%' || :query || '%'
        ORDER BY parsedAt DESC
        """,
    )
    fun pagingSearchLoads(query: String): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE isDispute = 1 AND disputeCompleted = 0 ORDER BY parsedAt DESC")
    fun pagingActiveDisputes(): androidx.paging.PagingSource<Int, LoadEntity>

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
            tripId = :tripId,
            date = :loadDate,
            totalRate = :totalRate,
            totalMiles = :totalMiles,
            pointA = :pointA,
            pointB = :pointB,
            puCount = :puCount,
            delCount = :delCount,
            weekNumber = :weekNumber,
            year = :year,
            rawMessage = :rawMessage,
            updatedAt = :updatedAt,
            firstPuMillis = :firstPuMillis,
            lastDelMillis = :lastDelMillis,
            route = :route,
            firstPuCityState = :firstPuCityState,
            lastDelCityState = :lastDelCityState,
            durationDays = :durationDays,
            pace = :pace,
            stopCount = :stopCount,
            isDispute = :isDispute,
            disputeResponseDate = :disputeResponseDate,
            disputeCompleted = :disputeCompleted,
            actualFinishDate = :actualFinishDate
        WHERE id = :loadId
        """
    )
    suspend fun update(
        loadId: String,
        tripId: String,
        loadDate: String,
        totalRate: Double,
        totalMiles: Double,
        pointA: String,
        pointB: String,
        puCount: Int,
        delCount: Int,
        weekNumber: Int,
        year: Int,
        rawMessage: String,
        updatedAt: Long,
        firstPuMillis: Long?,
        lastDelMillis: Long?,
        route: String,
        firstPuCityState: String,
        lastDelCityState: String,
        durationDays: Double,
        pace: Double,
        stopCount: Int,
        isDispute: Boolean,
        disputeResponseDate: String?,
        disputeCompleted: Boolean,
        actualFinishDate: String?,
    )

    /** Date/week repair without rewriting stops or penalties. */
    @Query(
        """
        UPDATE loads SET
            date = :loadDate,
            weekNumber = :weekNumber,
            year = :year,
            updatedAt = :updatedAt,
            firstPuMillis = :firstPuMillis,
            lastDelMillis = :lastDelMillis,
            durationDays = :durationDays,
            pace = :pace
        WHERE id = :loadId
        """
    )
    suspend fun updateCalendarFields(
        loadId: String,
        loadDate: String,
        weekNumber: Int,
        year: Int,
        updatedAt: Long,
        firstPuMillis: Long?,
        lastDelMillis: Long?,
        durationDays: Double,
        pace: Double,
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

    @Query("SELECT * FROM loads WHERE year = :weekYear ORDER BY date DESC, parsedAt DESC")
    suspend fun getLoadsByWeekYearOnce(weekYear: Int): List<LoadEntity>

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

    @Query(
        """
        SELECT
            COUNT(*) AS totalLoads,
            COALESCE(SUM(totalMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        """
    )
    fun watchTotalLoadStats(): Flow<LoadStatsAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE weekNumber = :weekNumber AND year = :year
        """
    )
    fun watchWeeklyLoadStats(weekNumber: Int, year: Int): Flow<WeeklyLoadStatsAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE weekNumber = :weekNumber AND year = :year
        """
    )
    suspend fun getWeeklyLoadStatsOnce(weekNumber: Int, year: Int): WeeklyLoadStatsAgg

    /** Loaded miles from journal loads with date on/after [startDate] (YYYY-MM-DD). */
    @Query("SELECT COALESCE(SUM(totalMiles), 0.0) FROM loads WHERE date >= :startDate")
    suspend fun sumMilesSince(startDate: String): Double

    /**
     * Prefer this for ТО math when finish dates are denormalized on the row:
     * end on/after service day (inclusive) — same rule as [com.truckerload.domain.maintenance.MaintenanceMileageUseCase].
     */
    @Query(
        """
        SELECT COALESCE(SUM(totalMiles), 0.0) FROM loads
        WHERE COALESCE(actualFinishDate, date) >= :serviceDate
        """,
    )
    suspend fun sumLoadedMilesOnOrAfterService(serviceDate: String): Double
}
