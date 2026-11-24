package com.truckerload.data.repository

import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.local.toEntity
import com.truckerload.domain.maintenance.MaintenanceProgressCalculator
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.sync.MaintenanceReminderCoordinator
import com.truckerload.utils.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
        // The load count is only a change trigger. Miles are summed per task in SQL.
        combine(dao.watchTasks(), loadDao.watchLoadCount()) { taskEntities, _ -> taskEntities }
            .mapLatest { taskEntities ->
                val today = LocalDate.now()
                taskEntities.map { it.toDomain() }
                    .filter { !it.isCompleted }
                    .map { task -> MaintenanceMileageQuery.progress(loadDao, task, today) }
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

    suspend fun insertArchives(entries: List<MaintenanceArchiveEntry>) {
        if (entries.isEmpty()) return
        dao.insertArchives(entries.map { it.copy(id = 0).toEntity() })
        scheduleAutoBackup()
    }

    suspend fun deleteArchive(id: Long) {
        deleteArchives(listOf(id))
    }

    suspend fun deleteArchives(ids: List<Long>) {
        if (ids.isEmpty()) return
        val photoPaths = ids.mapNotNull { id ->
            dao.getArchiveById(id)?.photoPath?.takeIf { it.isNotBlank() }
        }.distinct()
        ids.forEach { dao.deleteArchive(it) }
        photoPaths.forEach { path ->
            runCatching { java.io.File(path).delete() }
        }
        scheduleAutoBackup()
    }

    /** Completed ТО expenses for a calendar year (by serviceDate YYYY-MM-DD). */
    suspend fun getArchiveForYear(year: Int): List<MaintenanceArchiveEntry> {
        val prefix = "%04d-".format(year)
        return dao.getAllArchiveOnce()
            .map { it.toDomain() }
            .filter { it.serviceDate.startsWith(prefix) }
            .sortedBy { it.serviceDate }
    }

    suspend fun getDueProgressForNotifications(today: LocalDate = LocalDate.now()): List<MaintenanceProgress> {
        val active = dao.getActiveTasksOnce().map { it.toDomain() }
        return active.map { task ->
            MaintenanceMileageQuery.progress(loadDao, task, today)
        }.filter { MaintenanceProgressCalculator.shouldNotify(it) }
    }

    private fun scheduleAutoBackup() {
        AppDatabase.hostContext()?.let { context ->
            BackupService.scheduleCreateAutoBackup(context)
            MaintenanceReminderCoordinator.enqueue(context)
        }
    }
}
