package com.truckerload.data.repository

import com.truckerload.data.local.dao.LoadDao
import com.truckerload.domain.maintenance.MaintenanceMileageUseCase
import com.truckerload.domain.maintenance.MaintenanceMileageUseCase.ServiceMiles
import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.ZoneId

/**
 * ТО progress with the miles summed in SQL ([LoadDao.sumServiceMiles]) instead of
 * reading every load into memory. Same numbers as [MaintenanceMileageUseCase.progressForTask].
 */
internal object MaintenanceMileageQuery {

    suspend fun serviceMiles(
        loadDao: LoadDao,
        serviceDate: String,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ServiceMiles {
        val window = MaintenanceMileageUseCase.serviceWindow(serviceDate, today, zoneId)
        val agg = loadDao.sumServiceMiles(
            serviceDate = window.serviceDate,
            endCap = window.endCapIso,
            startMillis = window.startMillis,
            endMillisExclusive = window.endMillisExclusive,
            maxLoadMiles = window.maxLoadMiles,
        )
        return ServiceMiles(agg.miles, agg.loadCount)
    }

    suspend fun progress(
        loadDao: LoadDao,
        task: MaintenanceTask,
        today: LocalDate = LocalDate.now(),
    ): MaintenanceProgress {
        val counted = if (task.reminderType == MaintenanceReminderType.MILES) {
            serviceMiles(loadDao, task.startDate, today)
        } else {
            ServiceMiles(0L, 0)
        }
        return MaintenanceMileageUseCase.progressForTask(task, counted, today)
    }
}
