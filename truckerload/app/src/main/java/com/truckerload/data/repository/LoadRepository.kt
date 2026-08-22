package com.truckerload.data.repository

import androidx.room.withTransaction
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map as mapPaging
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.local.entities.LoadStatsAgg
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.WeekYieldAgg
import com.truckerload.data.local.entities.WeeklyLoadStatsAgg
import com.truckerload.data.sync.MediaSyncEnqueuer
import com.truckerload.domain.attach.AttachLoadSelection
import com.truckerload.domain.goal.WeekYieldSnapshot
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.parser.StopsHasher
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getLastDeliveryMillis
import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.withReportingWeek
import com.truckerload.utils.BackupService
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.LoadDateRepair
import com.truckerload.domain.parser.ParseUtils
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class SyncLoadsResult(
    val addedCount: Int,
    val lastAddedText: String,
    val status: SyncStatus
)

enum class SyncStatus { SUCCESS, DUPLICATE, EMPTY }

@OptIn(ExperimentalCoroutinesApi::class)
class LoadRepository(
    private val db: AppDatabase,
    private val mediaSync: MediaSyncEnqueuer = MediaSyncEnqueuer.forDatabase(db),
) {

    private val loadDao = db.loadDao()
    private val loadHistoryDao = db.loadHistoryDao()
    private val stopDao = db.stopDao()
    private val penaltyDao = db.penaltyDao()
    private val photoDao = db.photoDao()
    private val scanDao = db.scanDao()
    private val writeBatchDepth = AtomicInteger(0)

    fun getAllLoads(): Flow<List<Load>> =
        loadDao.getAllLoads().hydrateOnIo(stopDao, penaltyDao)

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
            .flowOn(Dispatchers.IO)

    /**
     * Clears photo/scan rows whose loadId no longer exists (and deletes orphan files).
     * Safe to call periodically after imports/restores.
     */
    suspend fun cleanupOrphanAttachments(): Int {
        val loadIds = loadDao.getAllLoadsOnce().map { it.id }.toSet()
        var removed = 0
        val syncEnabled = mediaSync.enabled()
        db.withTransaction {
            val orphanPhotos = photoDao.getAllPhotosOnce().filter { photo ->
                val id = photo.loadId
                !id.isNullOrBlank() && id !in loadIds
            }
            orphanPhotos.forEach { photo ->
                if (syncEnabled) mediaSync.enqueuePhotoDelete(photo)
                photoDao.deleteById(photo.id)
                runCatching { File(photo.filePath).delete() }
                removed++
            }
            val orphanScans = scanDao.getAllScansOnce().filter { scan ->
                val id = scan.loadId
                !id.isNullOrBlank() && id !in loadIds
            }
            orphanScans.forEach { scan ->
                if (syncEnabled) mediaSync.enqueueScanDelete(scan)
                scanDao.deleteById(scan.id)
                runCatching { File(scan.filePath).delete() }
                removed++
            }
        }
        if (syncEnabled && removed > 0) mediaSync.schedule()
        return removed
    }

    fun watchActualDailyYield(weekNumber: Int, year: Int): Flow<Double> =
        loadDao.watchActualDailyYield(weekNumber, year).flowOn(Dispatchers.IO)

    fun getLoadsByMonth(monthPrefix: String): Flow<List<Load>> =
        loadDao.getLoadsByMonth(monthPrefix).hydrateOnIo(stopDao, penaltyDao)

    fun searchLoads(query: String): Flow<List<Load>> =
        loadDao.searchLoads(query).hydrateOnIo(stopDao, penaltyDao)

    fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<Load>> =
        loadDao.getLoadsByWeek(weekNumber, year).hydrateOnIo(stopDao, penaltyDao)

    /** Точная дата (load_date). */
    fun getLoadsByDate(loadDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDate(loadDate).hydrateOnIo(stopDao, penaltyDao)

    /** Диапазон дат (включительно). */
    fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDateRange(startDate, endDate).hydrateOnIo(stopDao, penaltyDao)

    /**
     * True Room [PagingSource] journal rows (entity → domain without stop hydrate —
     * list cards use denormalized route fields; detail screen hydrates stops).
     */
    fun pagingLoads(
        weekNumber: Int? = null,
        year: Int? = null,
        startDate: String? = null,
        endDate: String? = null,
        exactDate: String? = null,
        searchQuery: String? = null,
        activeDisputesOnly: Boolean = false,
    ): Flow<PagingData<Load>> {
        val trimmedSearch = searchQuery?.trim().orEmpty()
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                enablePlaceholders = false,
                prefetchDistance = 20,
            ),
            pagingSourceFactory = {
                when {
                    trimmedSearch.isNotEmpty() -> loadDao.pagingSearchLoads(trimmedSearch)
                    activeDisputesOnly -> loadDao.pagingActiveDisputes()
                    weekNumber != null && year != null -> loadDao.pagingLoadsByWeek(weekNumber, year)
                    !exactDate.isNullOrBlank() -> loadDao.pagingLoadsByDate(exactDate)
                    !startDate.isNullOrBlank() && !endDate.isNullOrBlank() ->
                        loadDao.pagingLoadsByDateRange(startDate, endDate)
                    else -> loadDao.pagingAllLoads()
                }
            },
        ).flow.map { pagingData ->
            pagingData.mapPaging { entity -> entity.toDomain() }
        }
    }

    suspend fun getLoadsByYear(year: Int): List<Load> =
        hydrateLoadEntities(loadDao.getLoadsByWeekYearOnce(year), stopDao, penaltyDao)

    suspend fun getLoadsByDateRangeOnce(startDate: String, endDate: String): List<Load> =
        getLoadsByDateRange(startDate, endDate).first()

    suspend fun getAllLoadsOnce(): List<Load> =
        hydrateLoadEntities(loadDao.getAllLoadsOnce(), stopDao, penaltyDao)

    suspend fun getLoadsForLinking(limit: Int = 50): List<Load> =
        hydrateLoadEntities(loadDao.getLoadsForLinking(limit.coerceAtLeast(1)), stopDao, penaltyDao)

    /**
     * Up to [limit] loads from the current trucking week (Sun–Sat), newest by date then
     * [Load.parsedAt]. Used by widget Camera/Scan quick pick — not global updatedAt order.
     */
    suspend fun getRecentLoadsThisWeek(limit: Int = 3): List<Load> {
        val (week, year) = getCurrentWeekNumberAndYear()
        val weekLoads = hydrateLoadEntities(loadDao.getLoadsByWeekOnce(week, year), stopDao, penaltyDao)
        return AttachLoadSelection.quickPickThisWeek(
            loads = weekLoads,
            weekNumber = week,
            year = year,
            limit = limit,
        )
    }

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
        runBatchWrite {
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

    /**
     * Coalesce widget/backup side effects around a burst of writes (chat-history import).
     * Nested Room transactions join; observers emit once when the outer transaction ends.
     */
    suspend fun <T> runBatchWrite(block: suspend () -> T): T {
        writeBatchDepth.incrementAndGet()
        try {
            return db.withTransaction { block() }
        } finally {
            if (writeBatchDepth.decrementAndGet() == 0) {
                notifyWidgetDataChanged()
                scheduleAutoBackup()
            }
        }
    }

    suspend fun getLoadById(loadId: String): Load? {
        val entity = loadDao.getLoadById(loadId) ?: return null
        return entity.toDomain(stopsFor(loadId), penaltiesFor(loadId))
    }

    suspend fun getByTripId(tripId: String): Load? {
        val raw = tripId.trim()
        if (raw.isBlank()) return null
        val entity = loadDao.getByTripId(raw)
            ?: loadDao.getByTripId(normalizeTripId(raw))
            ?: return null
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
        val now = System.currentTimeMillis()
        val parsedAt = load.parsedAt.takeIf { it >= 946_684_800_000L } ?: now
        val repaired = LoadDateRepair.repair(
            load = load.copy(parsedAt = parsedAt),
            referenceMillis = parsedAt,
        )
        val normalized = repaired.copy(tripId = normalizeTripId(repaired.tripId)).withReportingWeek().withRouteMetrics()
        db.withTransaction {
            // FIX: REPLACE on loads would orphan autogen stop/penalty rows
            stopDao.deleteByLoadId(normalized.id)
            penaltyDao.deleteByLoadId(normalized.id)
            loadDao.insert(normalized.toEntity())
            stopDao.insertAll(normalized.stops.map { it.toEntity(normalized.id) })
            penaltyDao.insertAll(normalized.penalties.map { it.toEntity(normalized.id) })
        }
        notifyWidgetDataChanged()
        scheduleAutoBackup()
        if (playFeedback) {
            FeedbackManager.onLoadAdded()
        }
        AppDatabase.applicationContext()?.let { ctx ->
            runCatching {
                com.truckerload.sync.OutboundSyncQueue.enqueueLoadUpsert(
                    ctx,
                    normalized.id,
                    org.json.JSONObject()
                        .put("totalRate", normalized.totalRate)
                        .put("totalMiles", normalized.totalMiles)
                        .put("pointA", normalized.pointA)
                        .put("pointB", normalized.pointB),
                )
            }
        }
    }

    suspend fun updateLoad(load: Load) {
        val normalized = load.copy(tripId = normalizeTripId(load.tripId)).withReportingWeek().withRouteMetrics()
        db.withTransaction {
            loadDao.update(
                loadId = normalized.id,
                tripId = normalized.tripId,
                loadDate = normalized.date,
                totalRate = normalized.totalRate,
                totalMiles = normalized.totalMiles,
                pointA = normalized.pointA,
                pointB = normalized.pointB,
                puCount = normalized.puCount,
                delCount = normalized.delCount,
                weekNumber = normalized.weekNumber,
                year = normalized.year,
                rawMessage = normalized.rawMessage,
                updatedAt = System.currentTimeMillis(),
                firstPuMillis = getFirstPickUpMillis(normalized),
                lastDelMillis = LoadYieldCalculator.resolveFinishMillis(normalized)
                    ?: getLastDeliveryMillis(normalized),
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
        AppDatabase.applicationContext()?.let { ctx ->
            runCatching {
                com.truckerload.sync.OutboundSyncQueue.enqueueLoadUpsert(
                    ctx,
                    normalized.id,
                    org.json.JSONObject()
                        .put("op", "update")
                        .put("totalRate", normalized.totalRate)
                        .put("totalMiles", normalized.totalMiles),
                )
            }
        }
    }

    suspend fun deleteLoad(loadId: String) {
        val syncEnabled = mediaSync.enabled()
        var hadMedia = false
        db.withTransaction {
            val photos = photoDao.getPhotosByLoadIdOnce(loadId)
            val scans = scanDao.getScansByLoadIdOnce(loadId)
            hadMedia = photos.isNotEmpty() || scans.isNotEmpty()
            if (syncEnabled) {
                photos.forEach { mediaSync.enqueuePhotoDelete(it) }
                scans.forEach { mediaSync.enqueueScanDelete(it) }
            }
            photoDao.deleteByLoadId(loadId)
            scanDao.deleteByLoadId(loadId)
            loadHistoryDao.deleteByLoadId(loadId)
            loadDao.deleteById(loadId)
            photos.forEach { runCatching { File(it.filePath).delete() } }
            scans.forEach { runCatching { File(it.filePath).delete() } }
        }
        if (syncEnabled && hadMedia) mediaSync.schedule()
        notifyWidgetDataChanged()
        scheduleAutoBackup()
    }

    suspend fun deleteAllLoads() {
        val syncEnabled = mediaSync.enabled()
        var hadMedia = false
        db.withTransaction {
            val photos = photoDao.getAllPhotosOnce()
            val scans = scanDao.getAllScansOnce()
            hadMedia = photos.isNotEmpty() || scans.isNotEmpty()
            if (syncEnabled) {
                photos.forEach { mediaSync.enqueuePhotoDelete(it) }
                scans.forEach { mediaSync.enqueueScanDelete(it) }
            }
            photoDao.deleteAll()
            scanDao.deleteAll()
            loadHistoryDao.deleteAll()
            loadDao.deleteAll()
            photos.forEach { runCatching { File(it.filePath).delete() } }
            scans.forEach { runCatching { File(it.filePath).delete() } }
        }
        if (syncEnabled && hadMedia) mediaSync.schedule()
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
        val messageYear = messageDateSeconds
            ?.let { formatDateFromUnixSeconds(it).take(4).toIntOrNull() }
        val loadEntities = mutableListOf<LoadEntity>()
        val stopEntities = mutableListOf<StopEntity>()

        for (load in toInsert) {
            val normalized = load.copy(tripId = normalizeTripId(load.tripId), parsedAt = parsedAt)
            // Anchor MM/DD year to message/parsedAt — not wall-clock at hydrate time.
            val repaired = LoadDateRepair.repair(normalized, messageYear, parsedAt)
            val dated = when {
                repaired.date.isBlank() && messageDateSeconds != null ->
                    repaired.copy(date = formatDateFromUnixSeconds(messageDateSeconds))
                else -> repaired
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
        if (writeBatchDepth.get() > 0) return
        AppDatabase.applicationContext()?.let { WidgetDataUpdater.updateWidgetData(it) }
    }

    private fun scheduleAutoBackup() {
        if (writeBatchDepth.get() > 0) return
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }

    /**
     * Persists [LoadDateRepair] corrections so SQL week/year filters match the journal.
     * Safe to call on session start; only writes rows that actually change, in one transaction.
     */
    suspend fun repairMislabeledLoadDates(): Int = withContext(Dispatchers.IO) {
        val fixed = persistRepairedLoadDates(db)
        if (fixed > 0) {
            notifyWidgetDataChanged()
            scheduleAutoBackup()
        }
        fixed
    }

    /** Fixes inflated Relay miles (e.g. 182781 → 1827.81) for suspect rows only. */
    suspend fun repairInflatedLoadedMiles(): Int {
        val entities = loadDao.getLoadsWithSuspectInflatedMiles()
        if (entities.isEmpty()) return 0
        return runBatchWrite {
            var fixed = 0
            for (entity in entities) {
                val sanitized = ParseUtils.sanitizeLoadedMiles(entity.totalMiles, entity.totalRate)
                if (kotlin.math.abs(sanitized - entity.totalMiles) > 0.009) {
                    val stops = stopDao.getStopsByLoadId(entity.id)
                    val penalties = penaltyDao.getPenaltiesByLoadId(entity.id)
                    val load = entity.toDomain(stops, penalties).copy(totalMiles = sanitized)
                    updateLoad(load)
                    fixed++
                }
            }
            fixed
        }
    }

    private fun WeekYieldAgg.toSnapshot(): WeekYieldSnapshot = WeekYieldSnapshot(totalGross = totalGross, totalActiveDays = totalActiveDays)
}
