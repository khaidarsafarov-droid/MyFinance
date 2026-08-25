package com.truckerload.utils

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.tax.PerDiemCalculator
import java.util.Locale

/**
 * Builds an Excel-compatible SpreadsheetML workbook (.xls) with separate sheets
 * for loads, diesel, per-diem, and maintenance — readable for accountants.
 */
object AccountantWorkbookBuilder {

    data class Input(
        val year: Int,
        val loads: List<Load>,
        val diesel: List<Diesel>,
        val perDiemDates: Set<String>,
        val maintenance: List<MaintenanceArchiveEntry>,
        val grossIncome: Double,
        val locale: Locale = Locale.US,
    )

    fun resolveSections(requested: Set<AccountantExportSection>): Set<AccountantExportSection> {
        if (AccountantExportSection.ALL in requested || requested.isEmpty()) {
            return setOf(
                AccountantExportSection.LOADS,
                AccountantExportSection.DIESEL,
                AccountantExportSection.PER_DIEM,
                AccountantExportSection.MAINTENANCE,
            )
        }
        return requested - AccountantExportSection.ALL
    }

    fun buildXml(input: Input, requested: Set<AccountantExportSection>): String {
        val sections = resolveSections(requested)
        val sheets = mutableListOf<String>()
        sheets += summarySheet(input, sections)
        if (AccountantExportSection.LOADS in sections) {
            sheets += loadsSheet(input)
        }
        if (AccountantExportSection.DIESEL in sections) {
            sheets += dieselSheet(input)
        }
        if (AccountantExportSection.PER_DIEM in sections) {
            sheets += perDiemSheet(input)
        }
        if (AccountantExportSection.MAINTENANCE in sections) {
            sheets += maintenanceSheet(input)
        }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine(
                """<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""" +
                        """ xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""",
            )
            appendLine(
                """  <Styles>
    <Style ss:ID="Header">
      <Font ss:Bold="1"/>
      <Interior ss:Color="#E6EDE9" ss:Pattern="Solid"/>
    </Style>
    <Style ss:ID="Money">
      <NumberFormat ss:Format="#,##0.00"/>
    </Style>
  </Styles>""",
            )
            sheets.forEach { append(it) }
            appendLine("</Workbook>")
        }
    }

    fun fileLabel(sections: Set<AccountantExportSection>): String {
        val resolved = resolveSections(sections)
        return when {
            resolved.size >= 4 -> "All"
            resolved.size == 1 -> when (resolved.first()) {
                AccountantExportSection.LOADS -> "Loads"
                AccountantExportSection.DIESEL -> "Diesel"
                AccountantExportSection.PER_DIEM -> "PerDiem"
                AccountantExportSection.MAINTENANCE -> "Maintenance"
                AccountantExportSection.ALL -> "All"
            }

            else -> "Custom"
        }
    }

    private fun summarySheet(input: Input, sections: Set<AccountantExportSection>): String {
        val loadsTotal = input.loads.sumOf { it.totalRate }
        val dieselTotal = input.diesel.sumOf { it.totalAmount }
        val perDiemDays = input.perDiemDates.size
        val perDiemTotal = PerDiemCalculator.amount(perDiemDays)
        val maintenanceTotal = input.maintenance.sumOf { it.amount }
        val rows = mutableListOf(
            listOf("${BrandConstants.DISPLAY_NAME} — отчёт для бухгалтера", ""),
            listOf("Год", input.year.toString()),
            listOf("Валовой доход (paycheck), USD", money(input.grossIncome, input.locale)),
            listOf("", ""),
        )
        if (AccountantExportSection.LOADS in sections) {
            rows += listOf("Грузов", input.loads.size.toString())
            rows += listOf("Сумма ставок грузов, USD", money(loadsTotal, input.locale))
        }
        if (AccountantExportSection.DIESEL in sections) {
            rows += listOf("Заправок дизеля", input.diesel.size.toString())
            rows += listOf("Дизель всего, USD", money(dieselTotal, input.locale))
        }
        if (AccountantExportSection.PER_DIEM in sections) {
            rows += listOf("Дней на рейсах (суточные)", perDiemDays.toString())
            rows += listOf(
                "Ставка суточных, USD/день",
                money(PerDiemCalculator.DAILY_RATE, input.locale)
            )
            rows += listOf("Суточные всего, USD", money(perDiemTotal, input.locale))
        }
        if (AccountantExportSection.MAINTENANCE in sections) {
            rows += listOf("Записей ТО", input.maintenance.size.toString())
            rows += listOf("ТО всего, USD", money(maintenanceTotal, input.locale))
        }
        rows += listOf("", "")
        rows += listOf(
            "Подсказка",
            "Каждый лист — отдельная таблица. Откройте в Excel / Google Таблицах. Отправьте через Telegram, WhatsApp или почту.",
        )
        return worksheet("Сводка", rows, headerRow = false)
    }

    private fun loadsSheet(input: Input): String {
        val header = listOf(
            "Дата",
            "Trip ID",
            "Маршрут",
            "Ставка, USD",
            "Мили",
            "RPM",
            "Дней на рейсе",
        )
        val body = input.loads
            .sortedBy { it.date }
            .map { load ->
                val metrics = load.withRouteMetrics()
                val miles = metrics.totalMiles
                val rate = metrics.totalRate
                val rpm = if (miles > 0) rate / miles else 0.0
                val days = PerDiemCalculator.uniqueOnDutyDates(listOf(load), input.year).size
                listOf(
                    metrics.date,
                    metrics.tripId,
                    formatLoadRoute(metrics),
                    money(rate, input.locale),
                    String.format(input.locale, "%.0f", miles),
                    money(rpm, input.locale),
                    days.toString(),
                )
            }
        return worksheet("Грузы", listOf(header) + body, headerRow = true)
    }

    private fun dieselSheet(input: Input): String {
        val header = listOf(
            "Неделя",
            "Год",
            "Начало недели",
            "Конец недели",
            "Сумма, USD",
            "Галлоны",
            "Цена / гал",
            "Цена со скидкой / гал",
            "Экономия, USD",
            "Место",
        )
        val body = input.diesel
            .sortedWith(compareBy({ it.year }, { it.weekNumber }))
            .map { d ->
                listOf(
                    d.weekNumber.toString(),
                    d.year.toString(),
                    d.weekStartDate,
                    d.weekEndDate,
                    money(d.totalAmount, input.locale),
                    d.gallons?.let { String.format(input.locale, "%.2f", it) }.orEmpty(),
                    d.pricePerGallon?.let { money(it, input.locale) }.orEmpty(),
                    d.discountPricePerGallon?.let { money(it, input.locale) }.orEmpty(),
                    d.savingsAmount?.let { money(it, input.locale) }.orEmpty(),
                    d.location.orEmpty(),
                )
            }
        return worksheet("Дизель", listOf(header) + body, headerRow = true)
    }

    private fun perDiemSheet(input: Input): String {
        val header = listOf("Дата на рейсе", "Ставка, USD", "Сумма, USD")
        val rate = PerDiemCalculator.DAILY_RATE
        val body = input.perDiemDates.sorted().map { date ->
            listOf(date, money(rate, input.locale), money(rate, input.locale))
        }
        val totalRow = listOf(
            "ИТОГО (${input.perDiemDates.size} дн.)",
            money(rate, input.locale),
            money(PerDiemCalculator.amount(input.perDiemDates.size), input.locale),
        )
        return worksheet("Суточные", listOf(header) + body + listOf(totalRow), headerRow = true)
    }

    private fun maintenanceSheet(input: Input): String {
        val header = listOf("Дата ТО", "Услуга", "Описание", "Сумма, USD")
        val body = input.maintenance.map { entry ->
            listOf(
                entry.serviceDate,
                entry.serviceName,
                entry.description,
                money(entry.amount, input.locale),
            )
        }
        val total = listOf(
            "ИТОГО",
            "",
            "",
            money(input.maintenance.sumOf { it.amount }, input.locale),
        )
        return worksheet("ТО", listOf(header) + body + listOf(total), headerRow = true)
    }

    private fun worksheet(name: String, rows: List<List<String>>, headerRow: Boolean): String {
        val safeName = name.take(31).replace(Regex("""[\\/*?:\[\]]"""), "-")
        return buildString {
            appendLine("""  <Worksheet ss:Name="${xmlEscape(safeName)}">""")
            appendLine("    <Table>")
            rows.forEachIndexed { index, row ->
                val style = if (headerRow && index == 0) """ ss:StyleID="Header"""" else ""
                appendLine("      <Row$style>")
                row.forEach { cell ->
                    appendLine(
                        """        <Cell><Data ss:Type="String">${xmlEscape(cell)}</Data></Cell>""",
                    )
                }
                appendLine("      </Row>")
            }
            appendLine("    </Table>")
            appendLine("  </Worksheet>")
        }
    }

    private fun money(value: Double, locale: Locale): String =
        String.format(locale, "%.2f", value)

    fun xmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
