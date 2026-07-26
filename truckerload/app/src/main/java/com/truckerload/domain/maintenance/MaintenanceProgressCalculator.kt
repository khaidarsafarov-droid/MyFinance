package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Pure helpers: estimate truck odometer from load miles since the task start date,
 * and decide when a ТО reminder is due.
 */
object MaintenanceProgressCalculator {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    fun progress(
        task: MaintenanceTask,
        milesDrivenSinceStart: Double,
        today: LocalDate = LocalDate.now(),
    ): MaintenanceProgress {
        val miles = milesDrivenSinceStart.coerceAtLeast(0.0)
        return when (task.reminderType) {
            MaintenanceReminderType.MILES -> {
                val odoStart = task.odometerAtStart ?: 0.0
                val interval = task.intervalMiles ?: 0.0
                val estimated = odoStart + miles
                val target = odoStart + interval
                val remaining = (target - estimated).coerceAtLeast(0.0)
                MaintenanceProgress(
                    task = task,
                    milesDrivenSinceStart = miles,
                    estimatedOdometer = estimated,
                    targetOdometer = target,
                    milesRemaining = remaining,
                    daysRemaining = null,
                    isDue = interval > 0 && estimated >= target,
                )
            }
            MaintenanceReminderType.DATE -> {
                val due = task.dueDate?.let { runCatching { LocalDate.parse(it, iso) }.getOrNull() }
                val daysLeft = due?.let { ChronoUnit.DAYS.between(today, it) }
                MaintenanceProgress(
                    task = task,
                    milesDrivenSinceStart = miles,
                    estimatedOdometer = task.odometerAtStart?.let { it + miles },
                    targetOdometer = null,
                    milesRemaining = null,
                    daysRemaining = daysLeft,
                    isDue = due != null && !today.isBefore(due),
                )
            }
        }
    }

    fun shouldNotify(progress: MaintenanceProgress): Boolean {
        if (progress.task.isCompleted) return false
        if (progress.task.notifiedAt != null) return false
        return progress.isDue
    }
}
