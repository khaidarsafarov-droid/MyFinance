package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.maintenance.MaintenanceMilesFromLoads
import com.truckerload.domain.maintenance.MaintenanceProgressCalculator
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MaintenanceRepository(
    private val db: AppDatabase,
) {
    private val dao = db.maintenanceDao()
    private val loadDao = db.loadDao()

    fun watchTasks(): Flow<List<MaintenanceTask>> =
        dao.watchTasks().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun watchArchive(): Flow<List<MaintenanceArchiveEntry>> =
        dao.watchArchive().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun watchActiveProgress(): Flow<List<MaintenanceProgress>> =
        combine(dao.watchTasks(), loadDao.getAllLoads()) { taskEntities, loadEntities ->
            val tasks = taskEntities.map { it.toDomain() }.filter { !it.isCompleted }
            val loadMiles = loadEntities.map { entity ->
                MaintenanceMilesFromLoads.LoadMiles(
                    tripId = entity.tripId,
                    id = entity.id,
                    date = entity.date,
                    totalMiles = entity.totalMiles,
                    parsedAt = entity.parsedAt,
                )
            }
            tasks.map { task ->
                val summed = MaintenanceMilesFromLoads.sumForTask(
                    taskCreatedAt = task.createdAt,
                    startDate = task.startDate,
                    loads = loadMiles,
                )
                MaintenanceProgressCalculator.progress(
                    task = task,
                    milesDrivenSinceStart = summed.miles,
                    loadsCounted = summed.loadCount,
                )
            }
        }.flowOn(Dispatchers.IO)

    suspend fun insertTask(task: MaintenanceTask): Long {
        val id = dao.insertTask(task.copy(id = 0).toEntity())
        scheduleAutoBackup()
        return id
    }

    suspend fun updateTask(task: MaintenanceTask) {
        dao.updateTask(task.copy(updatedAt = System.currentTimeMillis()).toEntity())
        scheduleAutoBackup()
    }

    suspend fun markCompleted(id: Long) {
        val existing = dao.getTaskById(id)?.toDomain() ?: return
        updateTask(
            existing.copy(
                isCompleted = true,
                completedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markNotified(id: Long) {
        val existing = dao.getTaskById(id)?.toDomain() ?: return
        updateTask(existing.copy(notifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(id: Long) {
        dao.deleteTask(id)
        scheduleAutoBackup()
    }

    suspend fun insertArchive(entry: MaintenanceArchiveEntry): Long {
        val id = dao.insertArchive(entry.copy(id = 0).toEntity())
        scheduleAutoBackup()
        return id
    }

    suspend fun deleteArchive(id: Long) {
        dao.deleteArchive(id)
        scheduleAutoBackup()
    }

    suspend fun sumMilesSince(startDate: String): Double = loadDao.sumMilesSince(startDate)

    suspend fun getDueProgressForNotifications(today: LocalDate = LocalDate.now()): List<MaintenanceProgress> {
        val active = dao.getActiveTasksOnce().map { it.toDomain() }
        val loadMiles = loadDao.getAllLoadsOnce().map { entity ->
            MaintenanceMilesFromLoads.LoadMiles(
                tripId = entity.tripId,
                id = entity.id,
                date = entity.date,
                totalMiles = entity.totalMiles,
                parsedAt = entity.parsedAt,
            )
        }
        return active.map { task ->
            val summed = MaintenanceMilesFromLoads.sumForTask(
                taskCreatedAt = task.createdAt,
                startDate = task.startDate,
                loads = loadMiles,
            )
            MaintenanceProgressCalculator.progress(
                task = task,
                milesDrivenSinceStart = summed.miles,
                today = today,
                loadsCounted = summed.loadCount,
            )
        }.filter { MaintenanceProgressCalculator.shouldNotify(it) }
    }

    private fun scheduleAutoBackup() {
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }
}
