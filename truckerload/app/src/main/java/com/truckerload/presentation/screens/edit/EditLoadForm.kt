package com.truckerload.presentation.screens.edit

import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.DisputePayout
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.lastDelDateTimeFromStops
import com.truckerload.domain.model.normalizeTripId
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.MoneyInput
import com.truckerload.domain.parser.DecimalInput
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.utils.withReportingWeek
import java.time.Instant
import java.time.ZoneId

/** Fields the edit screen shows. Everything else on [Load] is left untouched. */
data class EditLoadFormState(
    val tripId: String,
    val loadDate: String,
    val finishDate: String,
    val totalRate: String,
    val totalMiles: String,
    val pointA: String,
    val pointB: String,
    val equipmentType: EquipmentType?,
    val disputeLoad: Load,
)

/**
 * Maps a stored load onto the edit form and back.
 * Opening the form and saving it without edits returns the same [Load].
 */
object EditLoadForm {
    fun moneyText(amount: Money): String = amount.formatPlain()

    /** Ignores a process-death draft that still holds `Money(cents=…)`. */
    fun rateField(saved: String?, amount: Money): String {
        if (saved.isNullOrBlank() || saved.startsWith("Money(")) return moneyText(amount)
        return saved
    }

    fun milesText(miles: Double, metric: Boolean): String {
        if (!miles.isFinite()) return ""
        val display = MeasurementUnits.displayDistance(miles, metric)
        return if (metric) MeasurementUnits.formatInput(display) else plainMiles(display)
    }

    fun finishText(load: Load, focusFinish: Boolean, nowMillis: Long): String {
        ActualFinishDate.normalize(load.actualFinishDate)?.let { return it }
        val fromStops = load.lastDelDateTimeFromStops().orEmpty()
        if (focusFinish && fromStops.length == 10) {
            return ActualFinishDate.combineNow(fromStops, nowMillis)
        }
        if (focusFinish && fromStops.isBlank()) {
            val today = Instant.ofEpochMilli(nowMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            return ActualFinishDate.combineNow(today, nowMillis)
        }
        return fromStops
    }

    fun fromLoad(
        load: Load,
        metric: Boolean = false,
        focusFinish: Boolean = false,
        nowMillis: Long = 0L,
    ): EditLoadFormState = EditLoadFormState(
        tripId = load.tripId,
        loadDate = load.date,
        finishDate = finishText(load, focusFinish, nowMillis),
        totalRate = moneyText(load.totalRate),
        totalMiles = milesText(load.totalMiles, metric),
        pointA = load.pointA,
        pointB = load.pointB,
        equipmentType = load.equipmentType,
        disputeLoad = load,
    )

    /**
     * @return null when the rate or miles text cannot be saved.
     * Unedited money, miles, dates, points, dispute, notes, and derived metrics stay as stored.
     */
    fun apply(
        original: Load,
        form: EditLoadFormState,
        metric: Boolean = false,
        nowMillis: Long,
    ): Load? {
        if (!rateOk(original, form.totalRate) || !milesOk(original, form.totalMiles, metric)) return null
        val dispute = form.disputeLoad
        var next = original.copy(
            tripId = if (form.tripId.isBlank() || form.tripId == original.tripId) {
                original.tripId
            } else {
                normalizeTripId(form.tripId)
            },
            date = form.loadDate.ifBlank { original.date },
            totalRate = resolveRate(original, form.totalRate),
            totalMiles = resolveMiles(original, form.totalMiles, metric),
            pointA = form.pointA,
            pointB = form.pointB,
            equipmentType = form.equipmentType,
            actualFinishDate = resolveFinish(original, form.finishDate),
            isDispute = dispute.isDispute,
            disputeResponseDate = dispute.disputeResponseDate,
            disputeCompleted = dispute.disputeCompleted,
            disputeAmount = dispute.disputeAmount,
            disputeApplyToLoad = dispute.disputeApplyToLoad,
            disputeAmountApplied = dispute.disputeAmountApplied,
        )
        next = DisputePayout.settleFrom(original, next)
        val scheduleChanged = next.actualFinishDate != original.actualFinishDate ||
            next.date != original.date ||
            next.stops != original.stops
        val rateChanged = next.totalRate != original.totalRate
        val pointsChanged = next.pointA != original.pointA || next.pointB != original.pointB
        if (next.stops != original.stops) {
            val metrics = next.withRouteMetrics()
            next = metrics.copy(pointA = next.pointA, pointB = next.pointB)
        } else if (scheduleChanged || rateChanged) {
            val duration = if (scheduleChanged) next.withRouteMetrics().durationDays else next.durationDays
            next = next.copy(durationDays = duration, pace = paceFor(next.totalRate, duration))
        } else if (pointsChanged && next.stops.isEmpty()) {
            val route = if (next.pointA.isNotBlank() && next.pointB.isNotBlank()) {
                "${next.pointA} → ${next.pointB}"
            } else {
                ""
            }
            next = next.copy(
                route = route,
                firstPuCityState = next.pointA,
                lastDelCityState = next.pointB,
            )
        }
        if (next.date != original.date || next.actualFinishDate != original.actualFinishDate) {
            val weeked = next.withReportingWeek()
            next = next.copy(weekNumber = weeked.weekNumber, year = weeked.year)
        }
        if (next == original) return original
        return next.copy(updatedAt = nowMillis)
    }

    private fun rateOk(original: Load, text: String): Boolean {
        if (text == moneyText(original.totalRate)) return true
        return MoneyInput.parse(text)?.isPositive() == true
    }

    private fun milesOk(original: Load, text: String, metric: Boolean): Boolean {
        if (text == milesText(original.totalMiles, metric)) return true
        val parsed = DecimalInput.parse(text) ?: return false
        return parsed >= 0.0
    }

    private fun resolveRate(original: Load, text: String): Money {
        if (text == moneyText(original.totalRate)) return original.totalRate
        return MoneyInput.parse(text) ?: original.totalRate
    }

    private fun resolveMiles(original: Load, text: String, metric: Boolean): Double {
        if (text == milesText(original.totalMiles, metric)) return original.totalMiles
        val parsed = DecimalInput.parse(text) ?: return original.totalMiles
        val stored = MeasurementUnits.storedDistance(parsed, metric)
        return if (stored == original.totalMiles) original.totalMiles else stored
    }

    private fun resolveFinish(original: Load, text: String): String? {
        val baseline = finishText(original, focusFinish = false, nowMillis = 0L)
        if (text == baseline) return original.actualFinishDate
        return ActualFinishDate.normalize(text)
    }

    /** Same pace rule as [com.truckerload.domain.model.withRouteMetrics], using the stored duration. */
    private fun paceFor(rate: Money, durationDays: Double): Money {
        if (durationDays <= 0.0) return Money.ZERO
        val days = kotlin.math.ceil(durationDays).toInt().coerceAtLeast(1)
        return rate.dividedBy(days)
    }

    private fun plainMiles(miles: Double): String {
        val raw = miles.toString()
        if (raw.contains('E', ignoreCase = true)) return MeasurementUnits.formatInput(miles)
        return if (raw.endsWith(".0")) raw.dropLast(2) else raw
    }
}
