package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/**
 * ТО mileage from journal loads.
 *
 * Formulas (all [Long] arithmetic — never string concat):
 * - targetOdometer = baseOdometer + targetInterval
 * - milesSinceService = Σ load.miles where endDate >= serviceDate
 *   AND the load was recorded after the reminder (so baseline odometer is not double-counted)
 * - currentOdometer = baseOdometer + milesSinceService
 * - remainingMiles = targetOdometer - currentOdometer
 *
 * Field mapping on [MaintenanceTask]:
 * - serviceDate → startDate
 * - baseOdometer → odometerAtStart
 * - targetInterval → intervalMiles
 */
object MaintenanceMileageUseCase {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    /** Ignore single-load outliers (corrupt / non-mile values). */
    private const val MAX_PLAUSIBLE_LOAD_MILES = 15_000L

    data class LoadInput(
        val tripId: String,
        val id: String,
        val miles: Double,
        val date: String,
        val actualFinishDate: String? = null,
        val lastDelMillis: Long? = null,
        /** When the load row entered the journal (ms). */
        val parsedAt: Long = 0L,
    )

    data class MileageSnapshot(
        val targetOdometer: Long,
        val totalDrivenMiles: Long,
        val currentOdometer: Long,
        val remainingMiles: Long,
        val milesSinceService: Long,
        val loadCount: Int,
        val isUrgent: Boolean,
        val progressFraction: Float,
    )

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

    fun milesAsLong(raw: Double): Long {
        if (!raw.isFinite() || raw <= 0.0) return 0L
        return raw.roundToLong().coerceAtMost(MAX_PLAUSIBLE_LOAD_MILES)
    }

    /**
     * @param serviceDate YYYY-MM-DD of the ТО / odometer snapshot
     * @param baselineRecordedAtMs when the reminder was saved — loads already in the journal
     *   before this moment are excluded (their miles are already inside [baseOdometer])
     * @param today caps end dates so future-misdated history cannot inflate the sum
     */
    fun calculate(
        baseOdometer: Long,
        targetInterval: Long,
        serviceDate: String,
        loads: List<LoadInput>,
        baselineRecordedAtMs: Long = 0L,
        today: LocalDate = LocalDate.now(),
    ): MileageSnapshot {
        val service = serviceDate.take(10)
        val todayIso = today.format(iso)
        val seen = LinkedHashSet<String>()
        var totalDrivenMiles = 0L
        var loadCount = 0

        for (load in loads) {
            val miles = milesAsLong(load.miles)
            if (miles <= 0L) continue

            // Baseline odometer already includes driving done before the ТО was logged.
            if (baselineRecordedAtMs > 0L && load.parsedAt > 0L && load.parsedAt < baselineRecordedAtMs) {
                continue
            }

            val endDate = resolveEndDate(load) ?: continue
            // Only trips that finished on/after the service day.
            if (endDate < service) continue
            // Drop absurd future end dates (common with year-misdated history).
            if (endDate > todayIso) continue

            val key = load.tripId.ifBlank { load.id }
            if (!seen.add(key)) continue

            totalDrivenMiles += miles
            loadCount++
        }

        val safeBase = baseOdometer.coerceAtLeast(0L)
        val safeInterval = targetInterval.coerceAtLeast(0L)
        val targetOdometer = safeBase + safeInterval
        val currentOdometer = safeBase + totalDrivenMiles
        val remainingMiles = targetOdometer - currentOdometer
        val isUrgent = safeInterval > 0L && remainingMiles <= 0L
        val progressFraction = if (safeInterval > 0L) {
            (totalDrivenMiles.toDouble() / safeInterval.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        return MileageSnapshot(
            targetOdometer = targetOdometer,
            totalDrivenMiles = totalDrivenMiles,
            currentOdometer = currentOdometer,
            remainingMiles = remainingMiles,
            milesSinceService = totalDrivenMiles,
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
                val base = (task.odometerAtStart ?: 0.0).roundToLong().coerceAtLeast(0L)
                val interval = (task.intervalMiles ?: 0.0).roundToLong().coerceAtLeast(0L)
                val snap = calculate(
                    baseOdometer = base,
                    targetInterval = interval,
                    serviceDate = task.startDate,
                    loads = loads,
                    baselineRecordedAtMs = task.createdAt,
                    today = today,
                )
                MaintenanceProgress(
                    task = task,
                    milesDrivenSinceStart = snap.milesSinceService.toDouble(),
                    estimatedOdometer = snap.currentOdometer.toDouble(),
                    targetOdometer = snap.targetOdometer.toDouble(),
                    milesRemaining = snap.remainingMiles.coerceAtLeast(0L).toDouble(),
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
