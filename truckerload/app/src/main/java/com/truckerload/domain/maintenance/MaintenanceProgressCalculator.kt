package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.LocalDate

/**
 * Thin wrapper kept for notifications / callers that already use this name.
 * Miles math lives in [MaintenanceMileageUseCase].
 */
object MaintenanceProgressCalculator {

    fun progress(
        task: MaintenanceTask,
        milesDrivenSinceStart: Double,
        today: LocalDate = LocalDate.now(),
        loadsCounted: Int = 0,
    ): MaintenanceProgress {
        // Legacy path: treat pre-summed miles as if they came from endDate >= serviceDate.
        val loads = if (milesDrivenSinceStart > 0 && task.reminderType == MaintenanceReminderType.MILES) {
            listOf(
                MaintenanceMileageUseCase.LoadInput(
                    tripId = "SUM",
                    id = "SUM",
                    miles = milesDrivenSinceStart,
                    date = task.startDate,
                    actualFinishDate = task.startDate,
                ),
            )
        } else {
            emptyList()
        }
        return MaintenanceMileageUseCase.progressForTask(task, loads, today).let { p ->
            if (loadsCounted > 0) p.copy(loadsCounted = loadsCounted) else p
        }
    }

    fun shouldNotify(progress: MaintenanceProgress): Boolean {
        if (progress.task.isCompleted) return false
        if (progress.task.notifiedAt != null) return false
        return progress.isDue
    }
}
