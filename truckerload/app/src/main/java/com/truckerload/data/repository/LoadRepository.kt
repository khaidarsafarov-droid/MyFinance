package com.truckerload.data.repository

import androidx.room.withTransaction
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.WeekYieldAgg
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.parser.StopsHasher
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.withReportingWeek
import com.truckerload.utils.BackupService
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.widget.WidgetDataUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.Locale

/** Результат CDC-синхронизации грузов. */
data class SyncLoadsResult(
    val addedCount: Int,
    val lastAddedText: String,
    val status: SyncStatus
)

enum class SyncStatus { SUCCESS, DUPLICATE, EMPTY }

@OptIn(ExperimentalCoroutinesApi::class)
class LoadRepository(private val db: AppDatabase) {

    private val loadDao = db.loadDao()
    private val loadHistoryDao = db.loadHistoryDao()
    private val stopDao = db.stopDao()
    private val penaltyDao = db.penaltyDao()
    private val photoDao = db.photoDao()
    private val scanDao = db.scanDao()

    /** Single Source of Truth: реактивный поток. Room эмитит при любом изменении таблицы loads. */
    fun getAllLoads(): Flow<List<Load>> =
        loadDao.getAllLoads()
            .mapLatest { hydrateLoads(it) }
            .flowOn(Dispatchers.IO)

    fun watchTotalLoadStats(): Flow<LoadStatsAgg> =
        loadDao.watchTotalLoadStats().flowOn(Dispatchers.IO)

    fun watchWeeklyLoadStats(weekNumber: Int, year: Int): Flow<WeeklyLoadStatsAgg> =
        loadDao.watchWeeklyLoadStats(weekNumber, year).flowOn(Dispatchers.IO)

    suspend fun getWeeklyLoadStatsOnce(weekNumber: Int, year: Int): WeeklyLoadStatsAgg =
        loadDao.getWeeklyLoadStatsOnce(weekNumber, year)

    /** Алиас для явной подписки (watch). Используй вместо разового getData(). */
    fun watchLoads(): Flow<List<Load>> = getAllLoads()

    /** SQL «двигатель эффективности» — пересчитывает неделю при смене календарной недели. */
    fun watchCurrentWeekYieldSnapshot(): Flow<WeekYieldSnapshot> =
        flow {
            while (true) {
                emit(getCurrentWeekNumberAndYear())
                delay(60_000L)
            }
        }
            .distinctUntilChanged()
            .flatMapLatest { (weekNumber, year) -> loadDao.watchWeekYieldAgg(weekNumber, year) }
            .map { it.toSnapshot() }

    /**
     * Clears photo/scan rows whose loadId no longer exists (and deletes orphan files).
     * Safe to call periodically after imports/restores.
     */
    suspend fun cleanupOrphanAttachments(): Int {
        val loadIds = loadDao.getAllLoadsOnce().map { it.id }.toSet()
        var removed = 0
        db.withTransaction {
            val orphanPhotos = photoDao.getAllPhotosOnce().filter { photo ->
                val id = photo.loadId
                !id.isNullOrBlank() && id !in loadIds
            }
            orphanPhotos.forEach { photo ->
                photoDao.deleteById(photo.id)
                runCatching { File(photo.filePath).delete() }
                removed++
            }
            val orphanScans = scanDao.getAllScansOnce().filter { scan ->
                val id = scan.loadId
                !id.isNullOrBlank() && id !in loadIds
            }
            orphanScans.forEach { scan ->
                scanDao.deleteById(scan.id)
                runCatching { File(scan.filePath).delete() }
                removed++
            }
        }
        return removed
    }

    fun watchActualDailyYield(weekNumber: Int, year: Int): Flow<Double> =
        loadDao.watchActualDailyYield(weekNumber, year)

    fun getLoadsByMonth(monthPrefix: String): Flow<List<Load>> =
        loadDao.getLoadsByMonth(monthPrefix).mapLatest { hydrateLoads(it) }

    fun searchLoads(query: String): Flow<List<Load>> =
        loadDao.searchLoads(query).mapLatest { hydrateLoads(it) }

    fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<Load>> =
        loadDao.getLoadsByWeek(weekNumber, year).mapLatest { hydrateLoads(it) }

    /** Точная дата (load_date). */
    fun getLoadsByDate(loadDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDate(loadDate).mapLatest { hydrateLoads(it) }

    /** Диапазон дат (включительно). */
    fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDateRange(startDate, endDate).mapLatest { hydrateLoads(it) }

    suspend fun getLoadsByYear(year: Int): List<Load> =
        getLoadsByDateRange("$year-01-01", "$year-12-31").first()

    suspend fun getLoadsByDateRangeOnce(startDate: String, endDate: String): List<Load> =
        getLoadsByDateRange(startDate, endDate).first()

    suspend fun getAllLoadsOnce(): List<Load> = hydrateLoads(loadDao.getAllLoadsOnce())

    suspend fun getLoadsForLinking(limit: Int = 50): List<Load> =
        hydrateLoads(loadDao.getLoadsForLinking(limit.coerceAtLeast(1)))

    /** Все грузы (разовый запрос). */
    suspend fun getAll(): List<Load> = getAllLoadsOnce()

    suspend fun importLoadsIfNotDuplicate(
        loads: List<Load>,
        parsedCount: Int
    ): com.truckerload.utils.LoadImporter.ImportResult {
        val incomingTripIds = loads.map { normalizeTripId(it.tripId) }.filter { it.isNotBlank() }
        val existingTripIds = loadDao.getExistingTripIds(incomingTripIds)
            .map { normalizeTripId(it) }
            .toMutableSet()
        var imported = 0
        var skipped = 0
        for (load in loads) {
            val tripId = normalizeTripId(load.tripId)
            if (tripId in existingTripIds) {
                skipped++
                continue
            }
            existingTripIds.add(tripId)
            insertLoad(load.copy(tripId = tripId), playFeedback = false)
            imported++
        }
        if (imported > 0) {
            FeedbackManager.onLoadAdded()
        }
        return com.truckerload.utils.LoadImporter.ImportResult(
            imported = imported,
            skipped = skipped,
            parsed = parsedCount
        )
    }

    suspend fun getLoadById(loadId: String): Load? {
        val entity = loadDao.getLoadById(loadId) ?: return null
        return entity.toDomain(stopsFor(loadId), penaltiesFor(loadId))
    }

    suspend fun getByTripId(tripId: String): Load? {
        val entity = loadDao.getByTripId(tripId.trim()) ?: return null
        return entity.toDomain(stopsFor(entity.id), penaltiesFor(entity.id))
    }

    suspend fun getByRouteAndDate(origin: String, destination: String, date: String): Load? {
        if (origin.isBlank() || destination.isBlank() || date.isBlank()) return null
        val entity = loadDao.getByRouteAndDate(origin, destination, date) ?: return null
        return entity.toDomain(stopsFor(entity.id), penaltiesFor(entity.id))
    }

    suspend fun getByStops(stops: List<Stop>, date: String): Load? {
        if (stops.isEmpty() || date.isBlank()) return null
        val targetHash = StopsHasher.calculateStopsHash(stops)
        for (entity in loadDao.getLoadsByDateOnce(date)) {
            val existingStops = stopDao.getStopsByLoadId(entity.id)
            val existingLoad = entity.toDomain(existingStops)
            if (StopsHasher.calculateStopsHash(existingLoad.stops) == targetHash) {
                return existingLoad
            }
        }
        return null
    }

    suspend fun update(load: Load) = updateLoad(load)

    suspend fun addChangeHistory(history: LoadHistory) {
        loadHistoryDao.insert(history)
    }

    suspend fun getChangeHistory(loadId: String): List<LoadHistory> =
        loadHistoryDao.getHistory(loadId)

    private suspend fun stopsFor(loadId: String) = stopDao.getStopsByLoadId(loadId)
    private suspend fun penaltiesFor(loadId: String) = penaltyDao.getPenaltiesByLoadId(loadId)

    suspend fun insertLoad(load: Load, playFeedback: Boolean = true) {
        val normalized = load.withReportingWeek().withRouteMetrics()
        db.withTransaction {
            loadDao.insert(normalized.toEntity())
            stopDao.insertAll(normalized.stops.map { it.toEntity(normalized.id) })
            penaltyDao.insertAll(normalized.penalties.map { it.toEntity(normalized.id) })
        }
        notifyWidgetDataChanged()
        scheduleAutoBackup()
        if (playFeedback) {
            FeedbackManager.onLoadAdded()
        }
    }

    suspend fun updateLoad(load: Load) {
        val normalized = load.withReportingWeek().withRouteMetrics()
        db.withTransaction {
            loadDao.update(
                loadId = normalized.id,
                loadDate = normalized.date,
                totalRate = normalized.totalRate,
                totalMiles = normalized.totalMiles,
                pointA = normalized.pointA,
                pointB = normalized.pointB,
                weekNumber = normalized.weekNumber,
                year = normalized.year,
                updatedAt = System.currentTimeMillis(),
                firstPuMillis = getFirstPickUpMillis(normalized),
                lastDelMillis = getLastDeliveryMillis(normalized),
                route = normalized.route,
                firstPuCityState = normalized.firstPuCityState,
                lastDelCityState = normalized.lastDelCityState,
                durationDays = normalized.durationDays,
                pace = normalized.pace,
                stopCount = normalized.stopCount,
                isDispute = normalized.isDispute,
                disputeResponseDate = normalized.disputeResponseDate,
                disputeCompleted = normalized.disputeCompleted,
                actualFinishDate = normalized.actualFinishDate,
            )
            stopDao.deleteByLoadId(normalized.id)
            penaltyDao.deleteByLoadId(normalized.id)
            if (normalized.stops.isNotEmpty()) {
                stopDao.insertAll(normalized.stops.map { it.toEntity(normalized.id) })
            }
            if (normalized.penalties.isNotEmpty()) {
                penaltyDao.insertAll(normalized.penalties.map { it.toEntity(normalized.id) })
            }
        }
        notifyWidgetDataChanged()
        scheduleAutoBackup()
    }

    suspend fun deleteLoad(loadId: String) {
        db.withTransaction {
            val photos = photoDao.getPhotosByLoadIdOnce(loadId)
            val scans = scanDao.getScansByLoadIdOnce(loadId)
            photoDao.deleteByLoadId(loadId)
            scanDao.deleteByLoadId(loadId)
            loadHistoryDao.deleteByLoadId(loadId)
            loadDao.deleteById(loadId)
            photos.forEach { runCatching { File(it.filePath).delete() } }
            scans.forEach { runCatching { File(it.filePath).delete() } }
        }
        notifyWidgetDataChanged()
        scheduleAutoBackup()
    }

    suspend fun deleteAllLoads() {
        db.withTransaction {
            val photos = photoDao.getAllPhotosOnce()
            val scans = scanDao.getAllScansOnce()
            photoDao.deleteAll()
            scanDao.deleteAll()
            loadDao.deleteAll()
            photos.forEach { runCatching { File(it.filePath).delete() } }
            scans.forEach { runCatching { File(it.filePath).delete() } }
        }
    }

    suspend fun refreshReportingWeeks() {
        for (load in getAllLoadsOnce()) {
            val normalized = load.withReportingWeek()
            if (normalized.weekNumber != load.weekNumber ||
                normalized.year != load.year ||
                normalized.date != load.date
            ) {
                updateLoad(normalized)
            }
        }
    }

    /** Пересчитывает маршрут, durationDays и pace из стопов. */
    suspend fun backfillRouteMetricsFromStops() {
        for (entity in loadDao.getAllLoads().first()) {
            val stops = stopDao.getStopsByLoadId(entity.id)
            val load = entity.toDomain(stops).withReportingWeek().withRouteMetrics()
            val updated = load.toEntity()
            if (updated.durationDays != entity.durationDays ||
                updated.route != entity.route ||
                updated.pace != entity.pace ||
                updated.firstPuMillis != entity.firstPuMillis ||
                updated.lastDelMillis != entity.lastDelMillis
            ) {
                loadDao.insert(updated)
            }
        }
        notifyWidgetDataChanged()
    }

    /** Заполняет firstPuMillis / lastDelMillis из стопов (после миграции БД). */
    suspend fun backfillPuDelMillisFromStops() {
        for (entity in loadDao.getAllLoads().first()) {
            if (entity.firstPuMillis != null && entity.lastDelMillis != null) continue
            val stops = stopDao.getStopsByLoadId(entity.id)
            if (stops.isEmpty()) continue
            val load = entity.toDomain(stops).withReportingWeek()
            loadDao.insert(load.toEntity())
        }
    }

    /**
     * CDC: синхронизация грузов. Вся логика в памяти — один запрос на проверку Trip ID, batch insert.
     * Не создаёт пустые записи и дубликаты.
     */
    suspend fun syncLoadsCdc(
        incomingLoads: List<Load>,
        messageDateSeconds: Long?,
        playFeedback: Boolean = true,
    ): SyncLoadsResult {
        val validLoads = incomingLoads.filter { load ->
            load.tripId.isNotBlank() && load.tripId != "T-UNKNOWN" &&
                (load.pointA.isNotBlank() || load.pointB.isNotBlank()) && load.totalRate > 0
        }
        if (validLoads.isEmpty()) {
            return SyncLoadsResult(0, "", SyncStatus.EMPTY)
        }

        val tripIds = validLoads.map { normalizeTripId(it.tripId) }
        val existingIds = loadDao.getExistingTripIds(tripIds).map { normalizeTripId(it) }.toSet()

        val toInsert = validLoads.filter { normalizeTripId(it.tripId) !in existingIds }
        if (toInsert.isEmpty()) {
            return SyncLoadsResult(0, "", SyncStatus.DUPLICATE)
        }

        val now = System.currentTimeMillis()
        val parsedAt = messageDateSeconds?.times(1000) ?: now
        val loadEntities = mutableListOf<LoadEntity>()
        val stopEntities = mutableListOf<StopEntity>()

        for (load in toInsert) {
            val dated = if (messageDateSeconds != null && load.date.isBlank()) {
                load.copy(
                    tripId = normalizeTripId(load.tripId),
                    date = formatDateFromUnixSeconds(messageDateSeconds),
                )
            } else {
                load.copy(tripId = normalizeTripId(load.tripId))
            }
            val loadWithWeek = dated.withReportingWeek().withRouteMetrics().copy(
                parsedAt = parsedAt,
                updatedAt = now,
            )
            loadEntities.add(loadWithWeek.toEntity())
            stopEntities.addAll(loadWithWeek.stops.map { it.toEntity(loadWithWeek.id) })
        }

        db.withTransaction {
            loadDao.insertAll(loadEntities)
            if (stopEntities.isNotEmpty()) stopDao.insertAll(stopEntities)
        }

        val lastAdded = toInsert.last()
        val lastAddedText = "${lastAdded.tripId} — ${lastAdded.pointA} → ${lastAdded.pointB}, $${String.format("%,.2f", lastAdded.totalRate)}"
        notifyWidgetDataChanged()
        scheduleAutoBackup()
        if (playFeedback && toInsert.isNotEmpty()) {
            FeedbackManager.onLoadAdded()
        }
        return SyncLoadsResult(toInsert.size, lastAddedText, SyncStatus.SUCCESS)
    }

    private fun notifyWidgetDataChanged() {
        AppDatabase.applicationContext()?.let { WidgetDataUpdater.updateWidgetData(it) }
    }

    private fun scheduleAutoBackup() {
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }

    private suspend fun hydrateLoads(entities: List<LoadEntity>): List<Load> {
        if (entities.isEmpty()) return emptyList()
        val loadIds = entities.map { it.id }
        // SQLite ограничивает IN(...) ~999 параметрами — батчим крупные флоты.
        val stopsByLoadId = loadIds.chunked(500)
            .flatMap { chunk -> stopDao.getStopsByLoadIds(chunk) }
            .groupBy { it.loadId }
        val penaltiesByLoadId = loadIds.chunked(500)
            .flatMap { chunk -> penaltyDao.getPenaltiesByLoadIds(chunk) }
            .groupBy { it.loadId }
        return entities.map { entity ->
            entity.toDomain(
                stops = stopsByLoadId[entity.id].orEmpty(),
                penalties = penaltiesByLoadId[entity.id].orEmpty(),
            )
        }
    }

    private fun WeekYieldAgg.toSnapshot(): WeekYieldSnapshot =
        WeekYieldSnapshot(totalGross = totalGross, totalActiveDays = totalActiveDays)
}
