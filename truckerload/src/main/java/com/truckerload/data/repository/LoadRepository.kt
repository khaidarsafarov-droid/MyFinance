package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.domain.model.Load
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Результат CDC-синхронизации грузов. */
data class SyncLoadsResult(
    val addedCount: Int,
    val lastAddedText: String,
    val status: SyncStatus
)

enum class SyncStatus { SUCCESS, DUPLICATE, EMPTY }

class LoadRepository(private val db: AppDatabase) {

    private val loadDao = db.loadDao()
    private val stopDao = db.stopDao()
    private val penaltyDao = db.penaltyDao()

    /** Single Source of Truth: реактивный поток. Room эмитит при любом изменении таблицы loads. */
    fun getAllLoads(): Flow<List<Load>> =
        loadDao.getAllLoads().map { list -> list.map { it.toDomain() } }

    /** Алиас для явной подписки (watch). Используй вместо разового getData(). */
    fun watchLoads(): Flow<List<Load>> = getAllLoads()

    fun getLoadsByMonth(monthPrefix: String): Flow<List<Load>> =
        loadDao.getLoadsByMonth(monthPrefix).map { list -> list.map { it.toDomain() } }

    fun searchLoads(query: String): Flow<List<Load>> =
        loadDao.searchLoads(query).map { list -> list.map { it.toDomain() } }

    fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<Load>> =
        loadDao.getLoadsByWeek(weekNumber, year).map { list -> list.map { it.toDomain() } }

    /** Точная дата (load_date). */
    fun getLoadsByDate(loadDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDate(loadDate).map { list -> list.map { it.toDomain() } }

    /** Диапазон дат (включительно). */
    fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<Load>> =
        loadDao.getLoadsByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    suspend fun getLoadsByYear(year: Int): List<Load> =
        getLoadsByDateRange("$year-01-01", "$year-12-31").first()

    suspend fun getLoadsByDateRangeOnce(startDate: String, endDate: String): List<Load> =
        getLoadsByDateRange(startDate, endDate).first()

    suspend fun getAllLoadsOnce(): List<Load> = getAllLoads().first()

    suspend fun getLoadById(loadId: String): Load? {
        val entity = loadDao.getLoadById(loadId) ?: return null
        return entity.toDomain(stopsFor(loadId), penaltiesFor(loadId))
    }

    private suspend fun stopsFor(loadId: String) = stopDao.getStopsByLoadId(loadId)
    private suspend fun penaltiesFor(loadId: String) = penaltyDao.getPenaltiesByLoadId(loadId)

    suspend fun insertLoad(load: Load) {
        loadDao.insert(load.toEntity())
        stopDao.insertAll(load.stops.map { it.toEntity(load.id) })
        penaltyDao.insertAll(load.penalties.map { it.toEntity(load.id) })
    }

    suspend fun updateLoad(load: Load) {
        loadDao.update(
            loadId = load.id,
            loadDate = load.date,
            totalRate = load.totalRate,
            totalMiles = load.totalMiles,
            pointA = load.pointA,
            pointB = load.pointB,
            weekNumber = load.weekNumber,
            year = load.year,
            updatedAt = System.currentTimeMillis()
        )
        stopDao.deleteByLoadId(load.id)
        penaltyDao.deleteByLoadId(load.id)
        if (load.stops.isNotEmpty()) stopDao.insertAll(load.stops.map { it.toEntity(load.id) })
        if (load.penalties.isNotEmpty()) penaltyDao.insertAll(load.penalties.map { it.toEntity(load.id) })
    }

    suspend fun deleteLoad(loadId: String) {
        loadDao.deleteById(loadId)
    }

    suspend fun deleteAllLoads() {
        loadDao.deleteAll()
    }

    /**
     * CDC: синхронизация грузов. Вся логика в памяти — один запрос на проверку Trip ID, batch insert.
     * Не создаёт пустые записи и дубликаты.
     */
    suspend fun syncLoadsCdc(
        incomingLoads: List<Load>,
        messageDateSeconds: Long?
    ): SyncLoadsResult {
        val validLoads = incomingLoads.filter { load ->
            load.tripId.isNotBlank() && load.tripId != "T-UNKNOWN" &&
                (load.pointA.isNotBlank() || load.pointB.isNotBlank()) && load.totalRate > 0
        }
        if (validLoads.isEmpty()) {
            return SyncLoadsResult(0, "", SyncStatus.EMPTY)
        }

        val tripIds = validLoads.map { it.tripId }
        val existingIds = loadDao.getExistingTripIds(tripIds).toSet()

        val toInsert = validLoads.filter { it.tripId !in existingIds }
        if (toInsert.isEmpty()) {
            return SyncLoadsResult(0, "", SyncStatus.DUPLICATE)
        }

        val now = System.currentTimeMillis()
        val loadEntities = mutableListOf<LoadEntity>()
        val stopEntities = mutableListOf<StopEntity>()
        val penaltyEntities = mutableListOf<PenaltyEntity>()

        for (load in toInsert) {
            val (weekNumber, year) = if (messageDateSeconds != null) {
                getWeekNumberAndYearFromDate(formatDateFromUnixSeconds(messageDateSeconds))
            } else {
                Pair(load.weekNumber, load.year)
            }
            val dateStr = if (messageDateSeconds != null) formatDateFromUnixSeconds(messageDateSeconds) else load.date
            val loadWithDate = load.copy(
                date = dateStr,
                weekNumber = weekNumber,
                year = year,
                parsedAt = now,
                updatedAt = now
            )
            loadEntities.add(loadWithDate.toEntity())
            stopEntities.addAll(loadWithDate.stops.map { it.toEntity(loadWithDate.id) })
            penaltyEntities.addAll(loadWithDate.penalties.map { it.toEntity(loadWithDate.id) })
        }

        loadDao.insertAll(loadEntities)
        if (stopEntities.isNotEmpty()) stopDao.insertAll(stopEntities)
        if (penaltyEntities.isNotEmpty()) penaltyDao.insertAll(penaltyEntities)

        val lastAdded = toInsert.last()
        val lastAddedText = "${lastAdded.tripId} — ${lastAdded.pointA} → ${lastAdded.pointB}, $${String.format("%,.2f", lastAdded.totalRate)}"
        return SyncLoadsResult(toInsert.size, lastAddedText, SyncStatus.SUCCESS)
    }
}
