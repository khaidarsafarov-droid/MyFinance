package com.truckerload.domain.report

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MiscExpense
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.sum
import java.time.LocalDate
import java.util.Locale

data class PeriodReportLoadDraft(
    val sortDate: String,
    val sortMillis: Long,
    val id: String,
    val heading: String,
    val pickup: String,
    val delivery: String,
    val reference: String,
    val shipOn: LocalDate? = null,
    val deliverOn: LocalDate? = null,
    val loadedMiles: Double,
    val emptyMiles: Double,
    val rate: Money,
    val rpm: Double?,
    val equipment: String,
    val status: String,
    val notes: String,
    val penalties: Money,
)

data class PeriodReportDraft(
    val driverName: String,
    val companyName: String = "",
    val periodLabel: String,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val weekNumber: Int? = null,
    val generatedOn: LocalDate,
    val locale: Locale,
    val loads: List<PeriodReportLoadDraft>,
    val fuel: Money,
    val fuelGallons: Double,
    val showFuel: Boolean,
    val expenses: Money,
    val showExpenses: Boolean,
    val goal: Money?,
)

object PeriodReportAssembler {

    fun assemble(
        loads: List<Load>,
        diesel: List<Diesel>,
        expenses: List<MiscExpense>,
        window: PeriodReportWindow,
        driverName: String,
        companyName: String = "",
        periodLabel: String,
        periodStart: LocalDate? = null,
        periodEnd: LocalDate? = null,
        weekNumber: Int? = null,
        generatedOn: LocalDate,
        locale: Locale,
        goal: Money?,
        labels: PeriodReportLabels,
        numbers: PeriodReportNumbers,
    ): PeriodReportDraft {
        val fuelRows = diesel.filter { it.belongsTo(window) }
        val expenseRows = expenses.filter { it.belongsTo(window) }
        return PeriodReportDraft(
            driverName = driverName.trim(),
            companyName = companyName.trim(),
            periodLabel = periodLabel,
            periodStart = periodStart,
            periodEnd = periodEnd,
            weekNumber = weekNumber,
            generatedOn = generatedOn,
            locale = locale,
            loads = loads.map { it.toDraft(labels, numbers) },
            fuel = if (window.includeFuel) fuelRows.map { it.totalAmount }.sum() else Money.ZERO,
            fuelGallons = if (window.includeFuel) {
                fuelRows.sumOf { it.gallons?.coerceAtLeast(0.0) ?: 0.0 }
            } else {
                0.0
            },
            showFuel = window.includeFuel,
            expenses = if (window.includeExpenses) expenseRows.map { it.amount }.sum() else Money.ZERO,
            showExpenses = window.includeExpenses,
            goal = goal?.takeIf { window.includeWeeklyGoal && it.isPositive() },
        )
    }

    private fun Load.toDraft(labels: PeriodReportLabels, numbers: PeriodReportNumbers): PeriodReportLoadDraft {
        val loaded = totalMiles.coerceAtLeast(0.0)
        val empty = deadheadMiles.coerceAtLeast(0.0)
        val driven = loaded + empty
        val start = parseIsoDate(date) ?: parseIsoDate(effectiveFinishDate())
        val finish = parseIsoDate(effectiveFinishDate())
        val heading = when {
            start != null && finish != null && finish.isAfter(start) ->
                "${numbers.date(start)} – ${numbers.date(finish)}"
            start != null -> numbers.date(start)
            finish != null -> numbers.date(finish)
            else -> date.ifBlank { PERIOD_REPORT_EMPTY_VALUE }
        }
        return PeriodReportLoadDraft(
            sortDate = start?.toString() ?: finish?.toString() ?: "9999-99-99",
            sortMillis = parsedAt,
            id = id,
            heading = heading,
            pickup = firstPuCityState.ifBlank { pointA }.trim(),
            delivery = lastDelCityState.ifBlank { pointB }.trim(),
            reference = tripId.trim(),
            shipOn = start,
            deliverOn = finish,
            loadedMiles = loaded,
            emptyMiles = empty,
            rate = totalRate,
            rpm = if (driven > 0.0) totalRate.cents / 100.0 / driven else null,
            equipment = equipmentType?.let { labels.equipmentNames[it] }.orEmpty(),
            status = when {
                isActiveDispute -> labels.disputeOpen
                hadDispute -> labels.disputeClosed
                else -> ""
            },
            notes = notesOf(labels, numbers),
            penalties = penalties.map { it.amount }.sum(),
        )
    }

    private fun Load.notesOf(labels: PeriodReportLabels, numbers: PeriodReportNumbers): String {
        val parts = mutableListOf<String>()
        stops.mapNotNull { it.note?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { parts += it }
        penalties.forEach { penalty ->
            val desc = penalty.description.trim().ifEmpty { labels.penalties }
            parts += "$desc (${numbers.money(penalty.amount)})"
        }
        val claimed = disputeAmount
        if (claimed != null && claimed.isPositive()) {
            parts += "${labels.disputeAmount}: ${numbers.money(claimed)}"
        }
        return parts.joinToString("; ")
    }
}

internal fun Diesel.belongsTo(window: PeriodReportWindow): Boolean {
    if (!window.includeFuel) return false
    if (window.kind == PeriodReportKind.WEEK) {
        return weekNumber == window.weekNumber && year == window.weekYear
    }
    val start = window.start ?: return window.kind == PeriodReportKind.ALL
    val end = window.end ?: start
    val rowStart = parseIsoDate(weekStartDate) ?: return false
    val rowEnd = parseIsoDate(weekEndDate) ?: rowStart
    return !rowEnd.isBefore(start) && !rowStart.isAfter(end)
}

internal fun MiscExpense.belongsTo(window: PeriodReportWindow): Boolean {
    if (!window.includeExpenses) return false
    val start = window.start ?: return window.kind == PeriodReportKind.ALL
    val end = window.end ?: start
    val day = parseIsoDate(date) ?: return false
    return !day.isBefore(start) && !day.isAfter(end)
}

internal fun parseIsoDate(raw: String?): LocalDate? {
    val text = raw?.trim()?.take(10) ?: return null
    if (text.length < 10) return null
    return runCatching { LocalDate.parse(text) }.getOrNull()
}
