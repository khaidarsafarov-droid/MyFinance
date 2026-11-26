package com.truckerload.domain.report

import com.truckerload.domain.money.Money
import com.truckerload.domain.money.sum
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object PeriodReportBuilder {

    fun build(
        draft: PeriodReportDraft,
        labels: PeriodReportLabels,
        numbers: PeriodReportNumbers,
    ): PeriodReport {
        val ordered = draft.loads.sortedWith(
            compareBy<PeriodReportLoadDraft> { it.sortDate }
                .thenBy { it.sortMillis }
                .thenBy { it.id },
        )
        val gross = ordered.map { it.rate }.sum()
        val loadedMiles = ordered.sumOf { it.loadedMiles }
        val emptyMiles = ordered.sumOf { it.emptyMiles }
        val totalMiles = loadedMiles + emptyMiles
        val penalties = ordered.map { it.penalties }.sum()
        val avgRpm = if (totalMiles > 0.0) gross.cents / 100.0 / totalMiles else 0.0
        var net = gross - penalties
        if (draft.showFuel) net -= draft.fuel
        if (draft.showExpenses) net -= draft.expenses
        val goal = draft.goal?.takeIf { it.isPositive() }
        val goalPercent = goal?.let { target ->
            ((gross.cents.toDouble() * 100.0) / target.cents.toDouble()).roundToInt()
        }
        val totals = PeriodReportTotals(
            gross = gross,
            loadCount = ordered.size,
            loadedMiles = loadedMiles,
            emptyMiles = emptyMiles,
            totalMiles = totalMiles,
            avgRpm = avgRpm,
            fuel = draft.fuel,
            fuelGallons = draft.fuelGallons,
            expenses = draft.expenses,
            penalties = penalties,
            net = net,
            goal = goal,
            goalPercent = goalPercent,
        )
        val header = buildList {
            if (draft.driverName.isNotEmpty()) add(ReportLine(labels.driver, draft.driverName))
            add(ReportLine(labels.period, draft.periodLabel))
            add(ReportLine(labels.generated, numbers.date(draft.generatedOn)))
        }
        val summary = summaryLines(totals, draft, labels, numbers)
        val blocks = ordered.mapIndexed { index, load ->
            ReportLoad(
                number = index + 1,
                heading = load.heading,
                rows = loadRows(load, labels, numbers, draft.locale),
            )
        }
        val statementRows = ordered.map { load ->
            val driven = load.loadedMiles + load.emptyMiles
            PeriodReportStatementRow(
                // The PDF statement is English-only: a cancelled load is marked "TONU" there.
                reference = load.reference.ifBlank { PERIOD_REPORT_EMPTY_VALUE }
                    .let { if (load.cancelled) "$it (TONU)" else it },
                origin = load.pickup.ifBlank { PERIOD_REPORT_EMPTY_VALUE },
                destination = load.delivery.ifBlank { PERIOD_REPORT_EMPTY_VALUE },
                shipDate = load.shipOn.toUs(usShortDate),
                deliveryDate = load.deliverOn.toUs(usShortDate),
                miles = compactMiles(driven),
                gross = usMoney(load.rate),
                ratePerMile = load.rpm?.let { usRpm(it) } ?: PERIOD_REPORT_EMPTY_VALUE,
            )
        }
        val emptyLoads = if (blocks.isEmpty()) labels.emptyLoads else null
        val report = PeriodReport(
            appName = labels.appName,
            title = labels.title,
            header = header,
            summaryTitle = labels.summary,
            summary = summary,
            loadsTitle = labels.loadsSection,
            emptyLoads = emptyLoads,
            loads = blocks,
            footer = labels.footer,
            totals = totals,
            plainText = "",
            driverName = draft.driverName,
            companyName = draft.companyName,
            statementRows = statementRows,
            statementMilesTotal = compactMiles(totalMiles),
            statementGrossTotal = usMoney(gross),
            statementAvgRate = if (totalMiles > 0.0) usRpm(avgRpm) else PERIOD_REPORT_EMPTY_VALUE,
            statementPeriod = englishPeriod(draft),
            statementGenerated = draft.generatedOn.format(usMediumDate),
            pdf = englishPdfLabels(),
        )
        return report.copy(plainText = renderText(report))
    }

    private val usMediumDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val usShortDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M/d/yy", Locale.US)

    private fun LocalDate?.toUs(format: DateTimeFormatter): String =
        this?.format(format)?.ifBlank { null } ?: PERIOD_REPORT_EMPTY_VALUE

    /** PDF money is always US dollars with a thousands separator, such as $1,971.00. */
    private fun usMoney(value: Money): String {
        val negative = value.cents < 0
        val absCents = kotlin.math.abs(value.cents)
        val dollars = String.format(Locale.US, "%,d", absCents / 100)
        val cents = (absCents % 100).toString().padStart(2, '0')
        return (if (negative) "-$" else "$") + dollars + "." + cents
    }

    private fun usRpm(perMile: Double): String =
        String.format(Locale.US, "$%.2f/mi", perMile)

    private fun englishPeriod(draft: PeriodReportDraft): String {
        val start = draft.periodStart ?: return draft.periodLabel
        val end = draft.periodEnd ?: start
        val range = if (end == start) {
            start.format(usMediumDate)
        } else {
            "${start.format(usMediumDate)} – ${end.format(usMediumDate)}"
        }
        val week = draft.weekNumber
        return if (week != null) "Week $week · $range" else range
    }

    private fun compactMiles(value: Double): String =
        if (value % 1.0 == 0.0) {
            String.format(Locale.US, "%,.0f", value)
        } else {
            String.format(Locale.US, "%,.1f", value)
        }

    private fun summaryLines(
        totals: PeriodReportTotals,
        draft: PeriodReportDraft,
        labels: PeriodReportLabels,
        numbers: PeriodReportNumbers,
    ): List<ReportLine> = buildList {
        add(ReportLine(labels.gross, numbers.money(totals.gross)))
        add(ReportLine(labels.loads, totals.loadCount.toString()))
        add(ReportLine(labels.loadedMiles, numbers.miles(totals.loadedMiles)))
        add(ReportLine(labels.emptyMiles, numbers.miles(totals.emptyMiles)))
        add(ReportLine(labels.totalMiles, numbers.miles(totals.totalMiles)))
        val rate = if (totals.totalMiles > 0.0) {
            numbers.rpm(totals.avgRpm)
        } else {
            PERIOD_REPORT_EMPTY_VALUE
        }
        add(ReportLine(labels.avgRate, rate))
        if (draft.showFuel) {
            add(ReportLine(labels.fuel, numbers.money(totals.fuel)))
            if (totals.fuelGallons > 0.0) {
                add(ReportLine(labels.fuelVolume, numbers.volume(totals.fuelGallons)))
            }
        }
        if (draft.showExpenses) {
            add(ReportLine(labels.expenses, numbers.money(totals.expenses)))
        }
        if (totals.penalties.isPositive()) {
            add(ReportLine(labels.penalties, numbers.money(totals.penalties)))
        }
        add(ReportLine(labels.net, numbers.money(totals.net)))
        val goal = totals.goal
        val percent = totals.goalPercent
        if (goal != null && percent != null) {
            add(
                ReportLine(
                    labels.goal,
                    "${numbers.money(totals.gross)} ${labels.goalJoin} ${numbers.money(goal)} ($percent%)",
                ),
            )
        }
    }

    private fun loadRows(
        load: PeriodReportLoadDraft,
        labels: PeriodReportLabels,
        numbers: PeriodReportNumbers,
        locale: Locale,
    ): List<ReportLine> = buildList {
        if (load.pickup.isNotEmpty()) add(ReportLine(labels.pickup, load.pickup))
        if (load.delivery.isNotEmpty()) add(ReportLine(labels.delivery, load.delivery))
        if (load.reference.isNotEmpty()) add(ReportLine(labels.reference, load.reference))
        val miles = if (load.emptyMiles > 0.0) {
            String.format(
                locale,
                labels.milesSplit,
                numbers.miles(load.loadedMiles),
                numbers.miles(load.emptyMiles),
            )
        } else {
            numbers.miles(load.loadedMiles)
        }
        add(ReportLine(labels.miles, miles))
        add(ReportLine(labels.rate, numbers.money(load.rate)))
        load.rpm?.let { add(ReportLine(labels.ratePerMile, numbers.rpm(it))) }
        if (load.equipment.isNotEmpty()) add(ReportLine(labels.equipment, load.equipment))
        if (load.status.isNotEmpty()) add(ReportLine(labels.status, load.status))
        if (load.notes.isNotEmpty()) add(ReportLine(labels.notes, load.notes))
    }

    fun renderText(report: PeriodReport): String = buildString {
        appendLine("${report.appName} \u2014 ${report.title}")
        report.header.forEach { appendLine("${it.label}: ${it.value}") }
        appendLine()
        appendLine(report.summaryTitle)
        report.summary.forEach { appendLine("${it.label}: ${it.value}") }
        appendLine()
        appendLine(report.loadsTitle)
        appendLine()
        if (report.emptyLoads != null) {
            appendLine(report.emptyLoads)
        } else {
            report.loads.forEachIndexed { index, load ->
                if (index > 0) {
                    appendLine()
                    appendLine(PERIOD_REPORT_LOAD_DIVIDER)
                    appendLine()
                }
                appendLine("${load.number}. ${load.heading}")
                load.rows.forEach { appendLine("${it.label}: ${it.value}") }
            }
        }
        appendLine()
        append(report.footer)
        appendLine()
    }
}
