package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.Diesel
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DieselRepository(private val db: AppDatabase) {

    private val dao = db.dieselDao()

    fun getAllDiesel(): Flow<List<Diesel>> =
        dao.getAllDiesel().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun getDieselForWeek(weekNumber: Int, year: Int): Flow<List<Diesel>> =
        dao.getDieselForWeek(weekNumber, year).map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    suspend fun getDieselForWeekOnce(weekNumber: Int, year: Int): List<Diesel> =
        getDieselForWeek(weekNumber, year).first()

    suspend fun getDieselById(id: Int): Diesel? =
        dao.getById(id)?.toDomain()

    suspend fun insertDiesel(diesel: Diesel) {
        dao.insert(diesel.toEntity())
        scheduleAutoBackup()
        AppDatabase.applicationContext()?.let { ctx ->
            runCatching {
                com.truckerload.sync.OutboundSyncQueue.enqueueDieselUpsert(
                    ctx,
                    diesel.id.toString(),
                    org.json.JSONObject()
                        .put("totalAmount", diesel.totalAmount)
                        .put("weekNumber", diesel.weekNumber)
                        .put("year", diesel.year),
                )
            }
        }
    }

    suspend fun deleteDiesel(id: Int) {
        dao.deleteById(id)
        scheduleAutoBackup()
    }

    /** Deletes all fills for the trucking week, then inserts [fills]. */
    suspend fun replaceWeekFills(weekNumber: Int, year: Int, fills: List<Diesel>) {
        val existing = getDieselForWeekOnce(weekNumber, year)
        existing.forEach { deleteDiesel(it.id) }
        fills.forEach { insertDiesel(it) }
    }

    suspend fun deleteAllDiesel() {
        dao.deleteAll()
    }

    suspend fun getDieselForYear(year: Int): List<Diesel> =
        getAllDiesel().first().filter { it.year == year }

    suspend fun getAllDieselOnce(): List<Diesel> = getAllDiesel().first()

    /** Overwrites week fields in place without bumping [Diesel.addedAt] or enqueueing sync. */
    suspend fun replaceReportingWeeks(updated: List<Diesel>) {
        if (updated.isEmpty()) return
        dao.insertAll(updated.map { it.toEntity() })
    }

    private fun scheduleAutoBackup() {
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }
}
