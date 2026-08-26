package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.BackupService
import com.truckerload.utils.PaycheckSourceFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class PaycheckRepository(private val db: AppDatabase) {

    private val dao = db.paycheckDao()

    fun getAllPaychecks(): Flow<List<Paycheck>> =
        dao.getAllPaychecks().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    suspend fun getPaycheckForWeek(weekNumber: Int, year: Int): Paycheck? =
        dao.getPaycheckForWeek(weekNumber, year)?.toDomain()

    fun getPaychecksForWeek(weekNumber: Int, year: Int): Flow<List<Paycheck>> =
        dao.getPaychecksForWeek(weekNumber, year).map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    suspend fun insertPaycheck(paycheck: Paycheck) {
        dao.insert(paycheck.toEntity())
        scheduleAutoBackup()
    }

    suspend fun updatePaycheck(paycheck: Paycheck) {
        dao.update(paycheck.toEntity())
        scheduleAutoBackup()
    }

    suspend fun deletePaycheck(id: Int) {
        val existing = dao.getById(id)
        dao.deleteById(id)
        existing?.sourceFilePath?.let { path ->
            AppDatabase.applicationContext()?.let { ctx ->
                PaycheckSourceFiles.delete(ctx, path)
            }
        }
        scheduleAutoBackup()
    }

    suspend fun deleteAllPaychecks() {
        dao.deleteAll()
    }

    suspend fun getPaychecksForYear(year: Int): List<Paycheck> =
        getAllPaychecks().first().filter { it.year == year }

    suspend fun getAllPaychecksOnce(): List<Paycheck> = getAllPaychecks().first()

    /** Overwrites week fields in place without bumping [Paycheck.addedAt]. */
    suspend fun replaceReportingWeeks(updated: List<Paycheck>) {
        if (updated.isEmpty()) return
        dao.insertAll(updated.map { it.toEntity() })
    }

    private fun scheduleAutoBackup() {
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }
}
