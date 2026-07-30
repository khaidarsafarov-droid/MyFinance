package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadMileageRow
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.maintenance.MaintenanceMileageUseCase
import com.truckerload.domain.maintenance.MaintenanceProgressCalculator
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceRepository(
    private val db: AppDatabase,
) {
    private val dao = db.maintenanceDao()
    private val loadDao = db.loadDao()

    fun watchTasks(): Flow<List<MaintenanceTask>> =
        dao.watchTasks().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    fun watchArchive(): Flow<List<MaintenanceArchiveEntry>> =
        dao.watchArchive().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.IO)

    /**
     * Active ТО progress. Miles tasks use a **narrow** load projection filtered by the
     * oldest service date (SQL), then [MaintenanceMileageUseCase] for dedupe / lastDel /
     * week-end cap. Full journal hydrate is avoided.
     */
    fun watchActiveProgress(): Flow<List<MaintenanceProgress>> =
        dao.watchTasks()
            .map { list -> list.map { it.toDomain() }.filter { !it.isCompleted } }
            .distinctUntilChanged()
            .flatMapLatest { tasks ->
                val milesTasks = tasks.filter { it.reminderType == MaintenanceReminderType.MILES }
                if (milesTasks.isEmpty()) {
                    flowOf(tasks.map { MaintenanceMileageUseCase.progressForTask(it, emptyList()) })
                } else {
                    val minService = milesTasks.minOf { it.startDate.take(10) }
                    val minEpoch = serviceDateToEpochMs(minService)
                    loadDao.watchLoadsForMileage(minService, minEpoch).map { rows ->
                        val loads = rows.map { it.toLoadInput() }
                        tasks.map { task -> MaintenanceMileageUseCase.progressForTask(task, loads) }
                    }
                }
            }
            .flowOn(Dispatchers.IO)

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
        val photoPath = dao.getArchiveById(id)?.photoPath
        dao.deleteArchive(id)
        photoPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { java.io.File(path).delete() }
        }
        scheduleAutoBackup()
    }

    suspend fun getDueProgressForNotifications(today: LocalDate = LocalDate.now()): List<MaintenanceProgress> {
        val active = dao.getActiveTasksOnce().map { it.toDomain() }
        val milesTasks = active.filter { it.reminderType == MaintenanceReminderType.MILES }
        val loads = if (milesTasks.isEmpty()) {
            emptyList()
        } else {
            val minService = milesTasks.minOf { it.startDate.take(10) }
            loadDao.getLoadsForMileageOnce(minService, serviceDateToEpochMs(minService))
                .map { it.toLoadInput() }
        }
        return active.map { task ->
            MaintenanceMileageUseCase.progressForTask(task, loads, today)
        }.filter { MaintenanceProgressCalculator.shouldNotify(it) }
    }

    private fun scheduleAutoBackup() {
        AppDatabase.applicationContext()?.let { BackupService.scheduleCreateAutoBackup(it) }
    }

    private fun LoadMileageRow.toLoadInput() = MaintenanceMileageUseCase.LoadInput(
        tripId = tripId,
        id = id,
        miles = totalMiles,
        date = date,
        actualFinishDate = actualFinishDate,
        lastDelMillis = lastDelMillis,
    )

    private fun serviceDateToEpochMs(serviceDate: String): Long {
        val day = runCatching { LocalDate.parse(serviceDate.take(10)) }.getOrNull()
            ?: return 0L
        return day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
