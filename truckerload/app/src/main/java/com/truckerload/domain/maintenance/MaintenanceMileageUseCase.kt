package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Smart ТО mileage: baseline odometer + sum of load miles whose trip **end date**
 * is on/after the service date.
 *
 * Mapping to stored [MaintenanceTask]:
 * - serviceDate → [MaintenanceTask.startDate]
 * - baseOdometer → [MaintenanceTask.odometerAtStart]
 * - targetInterval → [MaintenanceTask.intervalMiles]
 */
object MaintenanceMileageUseCase {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    data class LoadInput(
        val tripId: String,
        val id: String,
        val miles: Double,
        /** Load start / PU date (YYYY-MM-DD), fallback when end is unknown. */
        val date: String,
        /** Driver override finish date (YYYY-MM-DD). */
        val actualFinishDate: String? = null,
        /** Denormalized last DEL millis from Room. */
        val lastDelMillis: Long? = null,
    )

    data class MileageSnapshot(
        val targetOdometer: Double,
        val totalDrivenMiles: Double,
        val currentOdometer: Double,
        val remainingMiles: Double,
        val milesSinceService: Double,
        val loadCount: Int,
        val isUrgent: Boolean,
        val progressFraction: Float,
    )

    /**
     * Resolve trip end date: actualFinishDate → lastDelMillis → load.date.
     */
    fun resolveEndDate(load: LoadInput, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        load.actualFinishDate
            ?.trim()
            ?.takeIf { it.length >= 10 }
            ?.take(10)
            ?.let { return it }
        load.lastDelMillis
            ?.takeIf { it > 0L }
            ?.let { ms ->
                return Instant.ofEpochMilli(ms).atZone(zoneId).toLocalDate().format(iso)
            }
        return load.date.takeIf { it.length >= 10 }?.take(10)
    }

    fun calculate(
        baseOdometer: Double,
        targetInterval: Double,
        serviceDate: String,
        loads: List<LoadInput>,
    ): MileageSnapshot {
        val service = serviceDate.take(10)
        val seen = LinkedHashSet<String>()
        var totalDrivenMiles = 0.0
        var loadCount = 0
        for (load in loads) {
            if (load.miles <= 0.0) continue
            val endDate = resolveEndDate(load) ?: continue
            if (endDate < service) continue
            val key = load.tripId.ifBlank { load.id }
            if (!seen.add(key)) continue
            totalDrivenMiles += load.miles
            loadCount++
        }
        val targetOdometer = baseOdometer + targetInterval
        val currentOdometer = baseOdometer + totalDrivenMiles
        val remainingMiles = targetOdometer - currentOdometer
        val milesSinceService = totalDrivenMiles
        val isUrgent = targetInterval > 0 && remainingMiles <= 0
        val progressFraction = if (targetInterval > 0) {
            (milesSinceService / targetInterval).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        return MileageSnapshot(
            targetOdometer = targetOdometer,
            totalDrivenMiles = totalDrivenMiles,
            currentOdometer = currentOdometer,
            remainingMiles = remainingMiles,
            milesSinceService = milesSinceService,
            loadCount = loadCount,
            isUrgent = isUrgent,
            progressFraction = progressFraction,
        )
    }

    fun progressForTask(
        task: MaintenanceTask,
        loads: List<LoadInput>,
        today: LocalDate = LocalDate.now(),
    ): MaintenanceProgress {
        return when (task.reminderType) {
            MaintenanceReminderType.MILES -> {
                val base = task.odometerAtStart ?: 0.0
                val interval = task.intervalMiles ?: 0.0
                val snap = calculate(
                    baseOdometer = base,
                    targetInterval = interval,
                    serviceDate = task.startDate,
                    loads = loads,
                )
                MaintenanceProgress(
                    task = task,
                    milesDrivenSinceStart = snap.milesSinceService,
                    estimatedOdometer = snap.currentOdometer,
                    targetOdometer = snap.targetOdometer,
                    milesRemaining = snap.remainingMiles.coerceAtLeast(0.0),
                    daysRemaining = null,
                    isDue = snap.isUrgent,
                    loadsCounted = snap.loadCount,
                    progressFraction = snap.progressFraction,
                )
            }
            MaintenanceReminderType.DATE -> {
                val due = task.dueDate?.let { runCatching { LocalDate.parse(it.take(10), iso) }.getOrNull() }
                val daysLeft = due?.let { ChronoUnit.DAYS.between(today, it) }
                val isDue = due != null && !today.isBefore(due)
                MaintenanceProgress(
                    task = task,
                    milesDrivenSinceStart = 0.0,
                    estimatedOdometer = task.odometerAtStart,
                    targetOdometer = null,
                    milesRemaining = null,
                    daysRemaining = daysLeft,
                    isDue = isDue,
                    loadsCounted = 0,
                    progressFraction = 0f,
                )
            }
        }
    }
}
