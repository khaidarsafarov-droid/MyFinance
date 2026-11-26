package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truckerload.data.local.entities.LoadDateSpan
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.local.entities.ServiceMilesAgg
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
        ORDER BY
          CASE WHEN totalRate = :rate THEN 0 ELSE 1 END,
          ABS(totalRate - :rate) ASC,
          parsedAt DESC
        LIMIT 1
        """
    )
    suspend fun getByRouteAndDate(
        origin: String,
        destination: String,
        date: String,
        rate: Long,
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
          AND totalRate > 0
          AND (totalRate * 1.0 / totalMiles) < 50
        """,
    )
    suspend fun getLoadsWithSuspectInflatedMiles(): List<LoadEntity>

    @Query("SELECT * FROM loads ORDER BY updatedAt DESC, parsedAt DESC LIMIT :limit")
    suspend fun getLoadsForLinking(limit: Int): List<LoadEntity>

    @Query(
        """
        SELECT * FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
          AND (:maxDate = '' OR date <= :maxDate)
        ORDER BY parsedAt DESC
        """,
    )
    suspend fun getLoadsSince(minDate: String, maxDate: String): List<LoadEntity>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    fun getAllLoads(): Flow<List<LoadEntity>>

    @Query("SELECT * FROM loads ORDER BY parsedAt DESC")
    fun pagingAllLoads(): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE date LIKE :yearPrefix || '-%' ORDER BY parsedAt DESC")
    fun pagingLoadsByYear(yearPrefix: String): androidx.paging.PagingSource<Int, LoadEntity>

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

    @Query("SELECT * FROM loads WHERE isDispute = 1 ORDER BY parsedAt DESC")
    fun pagingDisputes(): androidx.paging.PagingSource<Int, LoadEntity>

    @Query("SELECT * FROM loads WHERE id = :loadId")
    suspend fun getLoadById(loadId: String): LoadEntity?

    @Query("SELECT * FROM loads WHERE date LIKE :monthPrefix ORDER BY parsedAt DESC")
    fun getLoadsByMonth(monthPrefix: String): Flow<List<LoadEntity>>

    @Query("SELECT * FROM loads WHERE tripId LIKE '%' || :query || '%' OR pointA LIKE '%' || :query || '%' OR pointB LIKE '%' || :query || '%' OR date LIKE '%' || :query || '%' ORDER BY parsedAt DESC")
    fun searchLoads(query: String): Flow<List<LoadEntity>>

    /**
     * REPLACE is DELETE + INSERT in SQLite: it cascades into stops/penalties. Callers must
     * re-insert children in the same transaction, or use [updateEntity] instead.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(load: LoadEntity)

    /** Plain UPDATE of an existing row; stops and penalties stay attached. */
    @Update
    suspend fun updateEntity(load: LoadEntity)

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
            deadheadMiles = :deadheadMiles,
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
            disputeAmount = :disputeAmount,
            disputeApplyToLoad = :disputeApplyToLoad,
            disputeAmountApplied = :disputeAmountApplied,
            actualFinishDate = :actualFinishDate
        WHERE id = :loadId
        """
    )
    suspend fun update(
        loadId: String,
        tripId: String,
        loadDate: String,
        totalRate: Long,
        totalMiles: Double,
        deadheadMiles: Double,
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
        pace: Long,
        stopCount: Int,
        isDispute: Boolean,
        disputeResponseDate: String?,
        disputeCompleted: Boolean,
        disputeAmount: Long?,
        disputeApplyToLoad: Boolean,
        disputeAmountApplied: Boolean,
        actualFinishDate: String?,
    )

    /**
     * Cancelled-load (TONU) state: [rate] and [miles] are the original rate (cents) and loaded
     * miles kept for undo. [update] writes the TONU rate and 0 loaded miles.
     */
    @Query(
        """
        UPDATE loads SET
            isCancelled = :cancelled,
            cancelledAt = :at,
            originalRate = :rate,
            originalMiles = :miles
        WHERE id = :id
        """,
    )
    suspend fun updateCancellation(id: String, cancelled: Boolean, at: Long?, rate: Long?, miles: Double?)

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
        pace: Long,
    )

    /** Точный поиск по load_date (YYYY-MM-DD). */
    @Query("SELECT * FROM loads WHERE date = :loadDate ORDER BY parsedAt DESC")
    fun getLoadsByDate(loadDate: String): Flow<List<LoadEntity>>

    /** Поиск по диапазону дат (включительно). */
    @Query("SELECT * FROM loads WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, parsedAt DESC")
    fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<LoadEntity>>

    /**
     * Loads whose PU–DEL span overlaps startMillis..endMillis, plus rows whose
     * `date` column falls in startDate..endDate (inclusive) when millis are missing.
     */
    @Query(
        """
        SELECT * FROM loads
        WHERE (date >= :startDate AND date <= :endDate)
           OR (
                firstPuMillis IS NOT NULL
                AND lastDelMillis IS NOT NULL
                AND firstPuMillis <= :endMillis
                AND lastDelMillis >= :startMillis
           )
        ORDER BY parsedAt DESC
        """,
    )
    fun getLoadsOverlappingRange(
        startDate: String,
        endDate: String,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<LoadEntity>>

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
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS miles,
            COUNT(*) AS loadCount
        FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
          AND (:maxDate = '' OR date <= :maxDate)
        GROUP BY year, weekNumber
        ORDER BY year ASC, weekNumber ASC
        """
    )
    suspend fun getWeeklyRevenue(minDate: String, maxDate: String): List<WeeklyRevenueAgg>

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
          AND (:maxDate = '' OR date <= :maxDate)
        GROUP BY dayOfWeek
        """
    )
    suspend fun getDailyDistribution(minDate: String, maxDate: String): List<DailyGrossAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalRate), 0.0) AS gross,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS miles
        FROM loads
        WHERE (:minDate = '' OR date >= :minDate)
          AND (:maxDate = '' OR date <= :maxDate)
        """
    )
    suspend fun getAnalyticsTotals(minDate: String, maxDate: String): AnalyticsTotalsAgg

    @Query(
        """
        SELECT
            COUNT(*) AS totalLoads,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        """
    )
    fun watchTotalLoadStats(): Flow<LoadStatsAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE weekNumber = :weekNumber AND year = :year
        """
    )
    fun watchWeeklyLoadStats(weekNumber: Int, year: Int): Flow<WeeklyLoadStatsAgg>

    @Query(
        """
        SELECT date AS startDate, actualFinishDate AS endDate
        FROM loads
        WHERE length(date) >= 10
        """
    )
    fun watchLoadDateSpans(): Flow<List<LoadDateSpan>>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE isDispute = 1
        """
    )
    fun watchDisputeLoadStats(): Flow<WeeklyLoadStatsAgg>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE date LIKE :yearPrefix || '-%'
        """
    )
    fun watchYearLoadStats(yearPrefix: String): Flow<WeeklyLoadStatsAgg>

    @Query("SELECT DISTINCT substr(date, 1, 4) FROM loads WHERE length(date) >= 4 ORDER BY 1 DESC")
    suspend fun getDistinctLoadYears(): List<String>

    @Query(
        """
        SELECT
            COUNT(*) AS loadCount,
            COALESCE(SUM(totalMiles + deadheadMiles), 0.0) AS totalMiles,
            COALESCE(SUM(totalRate), 0.0) AS totalRevenue
        FROM loads
        WHERE weekNumber = :weekNumber AND year = :year
        """
    )
    suspend fun getWeeklyLoadStatsOnce(weekNumber: Int, year: Int): WeeklyLoadStatsAgg

    /** Loaded miles from journal loads with date on/after [startDate] (YYYY-MM-DD). */
    @Query("SELECT COALESCE(SUM(totalMiles + deadheadMiles), 0.0) FROM loads WHERE date >= :startDate")
    suspend fun sumMilesSince(startDate: String): Double

    /**
     * Prefer this for ТО math when finish dates are denormalized on the row:
     * end on/after service day (inclusive) — same rule as [com.truckerload.domain.maintenance.MaintenanceMileageUseCase].
     */
    @Query(
        """
        SELECT COALESCE(SUM(totalMiles + deadheadMiles), 0.0) FROM loads
        WHERE COALESCE(actualFinishDate, date) >= :serviceDate
        """,
    )
    suspend fun sumLoadedMilesOnOrAfterService(serviceDate: String): Double

    /**
     * ТО miles in SQL, so reminders and the ТО screen do not read every load.
     * Mirrors [com.truckerload.domain.maintenance.MaintenanceMileageUseCase.calculate]:
     * - miles per load are rounded, loads under half a mile skipped, each load capped at [maxLoadMiles];
     * - end date is actualFinishDate, else lastDelMillis, else date;
     * - the load counts when that end is in [serviceDate]..[endCap] (inclusive).
     * The lastDelMillis day is resolved by the caller into [startMillis]..[endMillisExclusive],
     * so the device time zone stays in Kotlin. tripId is unique, so no dedup is needed here.
     */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN m > :maxLoadMiles THEN :maxLoadMiles ELSE m END), 0) AS miles,
            COUNT(*) AS loadCount
        FROM (
            SELECT
                CAST(ROUND(totalMiles) AS INTEGER) AS m,
                CASE
                    WHEN actualFinishDate IS NOT NULL
                        AND length(trim(actualFinishDate, ' ' || char(9, 10, 13))) >= 10
                        THEN substr(trim(actualFinishDate, ' ' || char(9, 10, 13)), 1, 10)
                    WHEN lastDelMillis IS NOT NULL AND lastDelMillis > 0
                        THEN CASE
                            WHEN lastDelMillis >= :startMillis AND lastDelMillis < :endMillisExclusive
                                THEN :serviceDate
                            ELSE NULL
                        END
                    WHEN length(date) >= 10 THEN substr(date, 1, 10)
                    ELSE NULL
                END AS endDate
            FROM loads
            WHERE totalMiles > 0 AND totalMiles < 9.0e15
        )
        WHERE m > 0 AND endDate IS NOT NULL AND endDate >= :serviceDate AND endDate <= :endCap
        """,
    )
    suspend fun sumServiceMiles(
        serviceDate: String,
        endCap: String,
        startMillis: Long,
        endMillisExclusive: Long,
        maxLoadMiles: Long,
    ): ServiceMilesAgg

    /** Emits on every change to the loads table. A cheap trigger for derived SQL sums. */
    @Query("SELECT COUNT(*) FROM loads")
    fun watchLoadCount(): Flow<Int>
}
