package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceProgress
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
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
 * - milesSinceService = Σ load.miles where **endDate >= serviceDate**
 *   (same-day finish after oil change counts; start/PU date is not used for the cutoff)
 * - currentOdometer = baseOdometer + milesSinceService
 * - remainingMiles = targetOdometer - currentOdometer
 *
 * Field mapping on [MaintenanceTask]:
 * - serviceDate → startDate
 * - baseOdometer → odometerAtStart
 * - targetInterval → intervalMiles
 *
 * [baseOdometer] is the reading **at service time**, so every trip that finished on/after
 * that day must count — even if it was already in the journal when the reminder was created.
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

    /**
     * Trip end date for ТО math: actualFinishDate → lastDelMillis → load.date.
     * Prefer finish/DEL so a multi-day trip that ends on/after service still counts.
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

    fun milesAsLong(raw: Double): Long {
        if (!raw.isFinite() || raw <= 0.0) return 0L
        return raw.roundToLong().coerceAtMost(MAX_PLAUSIBLE_LOAD_MILES)
    }

    /**
     * Inclusive upper bound for end dates: Saturday of the trucking week that contains [today]
     * (Sun–Sat). Lets same-week finishes count even if slightly ahead of the device clock.
     */
    fun inclusiveEndDateCap(today: LocalDate = LocalDate.now()): String {
        val (week, year) = getWeekNumberAndYearFromDate(today.format(iso))
        val (_, weekEnd, _) = getWeekRange(week, year)
        return weekEnd
    }

    /**
     * @param serviceDate YYYY-MM-DD of the ТО / odometer snapshot
     * @param today used to resolve the current trucking-week end cap (far-future misdates still drop)
     */
    fun calculate(
        baseOdometer: Long,
        targetInterval: Long,
        serviceDate: String,
        loads: List<LoadInput>,
        today: LocalDate = LocalDate.now(),
    ): MileageSnapshot {
        val service = serviceDate.take(10)
        // endDate >= serviceDate AND endDate <= end of current trucking week
        val endCapIso = inclusiveEndDateCap(today)
        val seen = LinkedHashSet<String>()
        var totalDrivenMiles = 0L
        var loadCount = 0

        for (load in loads) {
            val miles = milesAsLong(load.miles)
            if (miles <= 0L) continue

            val endDate = resolveEndDate(load) ?: continue
            // Same calendar day as ТО (or later) counts — not strict greater-than.
            if (endDate < service) continue
            // Drop absurd future / year-misdated ends beyond this trucking week.
            if (endDate > endCapIso) continue

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
