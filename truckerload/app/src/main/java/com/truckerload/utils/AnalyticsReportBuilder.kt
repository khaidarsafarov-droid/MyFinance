package com.truckerload.utils

import com.truckerload.data.repository.AnalyticsDashboard
import com.truckerload.presentation.utils.MoneyFormat
import java.util.Locale

/** Localized headings for a shareable analytics report. */
data class AnalyticsExportLabels(
    val appName: String,
    val title: String,
    val period: String,
    val periodLine: String,
    val summarySection: String,
    val totalLoads: String,
    val totalGross: String,
    val totalMiles: String,
    val avgRpm: String,
    val avgPerLoad: String,
    val bestWeek: String,
    val financeSection: String,
    val paycheck: String,
    val diesel: String,
    val gallons: String,
    val avgPrice: String,
    val savings: String,
    val weeklySection: String,
    val weekColumn: String,
    val yearColumn: String,
    val amountColumn: String,
    val milesColumn: String,
    val loadsColumn: String,
    val routesSection: String,
    val routeColumn: String,
    val rpmColumn: String,
    val dailySection: String,
    val dayColumn: String,
    val empty: String,
    val ownerLabel: String,
    val ownerName: String,
)

enum class AnalyticsShareFormat {
    TEXT,
    CSV,
}

/** Human-readable text and spreadsheet bodies for the My numbers share sheet. */
object AnalyticsReportBuilder {

    fun buildReadableText(
        dashboard: AnalyticsDashboard,
        labels: AnalyticsExportLabels,
    ): String = buildString {
        appendLine("${labels.appName} — ${labels.title}")
        appendLine(labels.periodLine)
        ownerLine(labels)?.let { appendLine(it) }
        appendLine()

        val s = dashboard.summary
        appendLine(labels.summarySection)
        appendLine("• ${labels.totalLoads}: ${s.totalLoads}")
        appendLine("• ${labels.totalGross}: ${MoneyFormat.formatCurrency(s.totalGross)}")
        appendLine("• ${labels.totalMiles}: ${MoneyFormat.formatMiles(s.totalMiles)}")
        appendLine("• ${labels.avgRpm}: ${MoneyFormat.formatRpm(s.avgRpm)}")
        appendLine("• ${labels.avgPerLoad}: ${MoneyFormat.formatCurrency(s.avgGrossPerLoad)}")
        val best = s.bestWeek
        if (best != null) {
            appendLine(
                "• ${labels.bestWeek}: ${MoneyFormat.formatCurrency(best.gross)} (${best.label})",
            )
        }
        appendLine()

        val f = dashboard.finance
        appendLine(labels.financeSection)
        appendLine("• ${labels.paycheck}: ${MoneyFormat.formatCurrency(f.paycheckTotal)}")
        appendLine("• ${labels.diesel}: ${MoneyFormat.formatCurrency(f.dieselTotal)}")
        if (f.dieselGallons > 0.0) {
            appendLine("• ${labels.gallons}: ${MoneyFormat.formatNumber(f.dieselGallons)}")
            f.avgPricePerGallon?.let { price ->
                appendLine("• ${labels.avgPrice}: ${String.format(Locale.US, "$%.2f", price)}")
            }
        }
        if (f.dieselSavings > 0.0) {
            appendLine("• ${labels.savings}: ${MoneyFormat.formatCurrency(f.dieselSavings)}")
        }
        appendLine()

        appendLine(labels.weeklySection)
        if (dashboard.weeks.isEmpty()) {
            appendLine(labels.empty)
        } else {
            dashboard.weeks.forEach { week ->
                appendLine(
                    "• ${week.label}: ${MoneyFormat.formatCurrency(week.gross)} · " +
                        "${MoneyFormat.formatMiles(week.miles)} · ${week.loadCount}",
                )
            }
        }
        appendLine()

        appendLine(labels.routesSection)
        if (dashboard.routes.isEmpty()) {
            appendLine(labels.empty)
        } else {
            dashboard.routes.forEach { route ->
                appendLine(
                    "• ${route.route}: ${MoneyFormat.formatCurrency(route.gross)} · " +
                        "${MoneyFormat.formatRpm(route.rpm)} · ${route.loadCount}",
                )
            }
        }
        appendLine()

        appendLine(labels.dailySection)
        if (dashboard.daily.isEmpty()) {
            appendLine(labels.empty)
        } else {
            dashboard.daily.forEach { day ->
                appendLine(
                    "• ${day.dayLabel}: ${MoneyFormat.formatCurrency(day.gross)} · ${day.loadCount}",
                )
            }
        }
    }

    fun buildCsvContent(
        dashboard: AnalyticsDashboard,
        labels: AnalyticsExportLabels,
    ): String = buildString {
        append('\uFEFF')
        appendLine("# ${labels.appName} — ${labels.title}")
        appendLine("# ${labels.periodLine}")
        ownerLine(labels)?.let { appendLine("# $it") }
        appendLine()

        val s = dashboard.summary
        appendLine(labels.summarySection)
        appendLine("${csv(labels.totalLoads)},${s.totalLoads}")
        appendLine("${csv(labels.totalGross)},${money(s.totalGross)}")
        appendLine("${csv(labels.totalMiles)},${number(s.totalMiles)}")
        appendLine("${csv(labels.avgRpm)},${money(s.avgRpm)}")
        appendLine("${csv(labels.avgPerLoad)},${money(s.avgGrossPerLoad)}")
        s.bestWeek?.let { best ->
            appendLine("${csv(labels.bestWeek)},${csv(best.label)},${money(best.gross)}")
        }
        appendLine()

        val f = dashboard.finance
        appendLine(labels.financeSection)
        appendLine("${csv(labels.paycheck)},${money(f.paycheckTotal)}")
        appendLine("${csv(labels.diesel)},${money(f.dieselTotal)}")
        appendLine("${csv(labels.gallons)},${number(f.dieselGallons)}")
        appendLine("${csv(labels.avgPrice)},${f.avgPricePerGallon?.let { money(it) } ?: ""}")
        appendLine("${csv(labels.savings)},${money(f.dieselSavings)}")
        appendLine()

        appendLine(labels.weeklySection)
        appendLine(
            listOf(
                labels.weekColumn,
                labels.yearColumn,
                labels.amountColumn,
                labels.milesColumn,
                labels.loadsColumn,
            ).joinToString(",") { csv(it) },
        )
        dashboard.weeks.forEach { week ->
            appendLine(
                listOf(
                    week.weekNumber.toString(),
                    week.year.toString(),
                    money(week.gross),
                    number(week.miles),
                    week.loadCount.toString(),
                ).joinToString(","),
            )
        }
        appendLine()

        appendLine(labels.routesSection)
        appendLine(
            listOf(
                labels.routeColumn,
                labels.amountColumn,
                labels.milesColumn,
                labels.rpmColumn,
                labels.loadsColumn,
            ).joinToString(",") { csv(it) },
        )
        dashboard.routes.forEach { route ->
            appendLine(
                listOf(
                    csv(route.route),
                    money(route.gross),
                    number(route.miles),
                    money(route.rpm),
                    route.loadCount.toString(),
                ).joinToString(","),
            )
        }
        appendLine()

        appendLine(labels.dailySection)
        appendLine(
            listOf(labels.dayColumn, labels.amountColumn, labels.loadsColumn)
                .joinToString(",") { csv(it) },
        )
        dashboard.daily.forEach { day ->
            appendLine(
                listOf(csv(day.dayLabel), money(day.gross), day.loadCount.toString())
                    .joinToString(","),
            )
        }
    }

    private fun ownerLine(labels: AnalyticsExportLabels): String? {
        val name = labels.ownerName.trim()
        if (name.isEmpty()) return null
        return "${labels.ownerLabel}: $name"
    }

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun number(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun csv(value: String): String {
        if (value.any { it == ',' || it == '"' || it == '\n' }) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }
}
