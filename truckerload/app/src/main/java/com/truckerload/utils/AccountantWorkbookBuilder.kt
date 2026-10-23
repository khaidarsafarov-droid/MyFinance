package com.truckerload.utils

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.money.sum
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.domain.tax.PerDiemCalculator
import java.time.LocalDate
import java.util.Locale

/**
 * Builds an Excel-compatible SpreadsheetML workbook (.xls) with separate sheets
 * for loads, diesel, per-diem, and maintenance — readable for accountants.
 *
 * Money, miles, gallons, RPM, and prices are numeric cells (dot decimal, no
 * grouping). Dates are DateTime cells. Text stays text, and missing values stay
 * blank so SUM and sorting work in any Excel locale.
 */
object AccountantWorkbookBuilder {

    data class Input(
        val year: Int,
        val loads: List<Load>,
        val diesel: List<Diesel>,
        val perDiemDates: Set<String>,
        val maintenance: List<MaintenanceArchiveEntry>,
        val grossIncome: Double,
        /** Caller hint only. Exported numbers always use a dot decimal ([Locale.ROOT]). */
        val locale: Locale = Locale.US,
        /** When true, distance and fuel cells are converted and headers use the labels below. */
        val metric: Boolean = false,
        val distanceHeader: String = "Мили",
        val volumeHeader: String = "Галлоны",
        val priceHeader: String = "Цена / гал",
        val discountHeader: String = "Цена со скидкой / гал",
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
            appendLine(WORKBOOK_STYLES)
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
        val loadsTotal = input.loads.map { it.totalRate }.sum()
        val dieselTotal = input.diesel.map { it.totalAmount }.sum()
        val perDiemDays = input.perDiemDates.size
        val perDiemTotal = PerDiemCalculator.amount(perDiemDays)
        val maintenanceTotal = input.maintenance.map { it.amount }.sum()
        val rows = mutableListOf(
            listOf(text("${BrandConstants.DISPLAY_NAME} — отчёт для бухгалтера"), blank()),
            listOf(text("Год"), integerCell(input.year)),
            listOf(text("Валовой доход (paycheck), USD"), moneyCell(input.grossIncome)),
            listOf(blank(), blank()),
        )
        if (AccountantExportSection.LOADS in sections) {
            rows += listOf(text("Грузов"), integerCell(input.loads.size))
            rows += listOf(text("Сумма ставок грузов, USD"), moneyCell(loadsTotal))
        }
        if (AccountantExportSection.DIESEL in sections) {
            rows += listOf(text("Заправок дизеля"), integerCell(input.diesel.size))
            rows += listOf(text("Дизель всего, USD"), moneyCell(dieselTotal))
        }
        if (AccountantExportSection.PER_DIEM in sections) {
            rows += listOf(text("Дней на рейсах (суточные)"), integerCell(perDiemDays))
            rows += listOf(text("Ставка суточных, USD/день"), moneyCell(PerDiemCalculator.DAILY_RATE))
            rows += listOf(text("Суточные всего, USD"), moneyCell(perDiemTotal))
        }
        if (AccountantExportSection.MAINTENANCE in sections) {
            rows += listOf(text("Записей ТО"), integerCell(input.maintenance.size))
            rows += listOf(text("ТО всего, USD"), moneyCell(maintenanceTotal))
        }
        rows += listOf(blank(), blank())
        rows += listOf(
            text("Подсказка"),
            text(
                "Каждый лист — отдельная таблица. Откройте в Excel / Google Таблицах. Отправьте через Telegram, WhatsApp или почту.",
            ),
        )
        return worksheet("Сводка", rows, headerRow = false)
    }

    private fun loadsSheet(input: Input): String {
        val header = listOf(
            text("Дата"),
            text("Trip ID"),
            text("Маршрут"),
            text("Ставка, USD"),
            text(if (input.metric) input.distanceHeader else "Мили"),
            text("RPM"),
            text("Дней на рейсе"),
        )
        val body = input.loads
            .sortedBy { it.date }
            .map { load ->
                val metrics = load.withRouteMetrics()
                val miles = metrics.totalMiles
                val rate = metrics.totalRate
                val rpm = if (miles > 0) rate.cents / 100.0 / miles else 0.0
                val shownMiles = if (input.metric) MeasurementUnits.displayDistance(miles, true) else miles
                val shownRpm = if (input.metric) MeasurementUnits.displayRatePerDistance(rpm, true) else rpm
                val days = PerDiemCalculator.uniqueOnDutyDates(listOf(load), input.year).size
                listOf(
                    dateCell(metrics.date),
                    text(metrics.tripId),
                    text(formatLoadRoute(metrics)),
                    moneyCell(rate),
                    milesCell(shownMiles),
                    decimal2Cell(shownRpm),
                    integerCell(days),
                )
            }
        return worksheet("Грузы", listOf(header) + body, headerRow = true)
    }

    private fun dieselSheet(input: Input): String {
        val header = listOf(
            text("Неделя"),
            text("Год"),
            text("Начало недели"),
            text("Конец недели"),
            text("Сумма, USD"),
            text(if (input.metric) input.volumeHeader else "Галлоны"),
            text(if (input.metric) input.priceHeader else "Цена / гал"),
            text(if (input.metric) input.discountHeader else "Цена со скидкой / гал"),
            text("Экономия, USD"),
            text("Место"),
        )
        val body = input.diesel
            .sortedWith(compareBy({ it.year }, { it.weekNumber }))
            .map { entry ->
                listOf(
                    integerCell(entry.weekNumber),
                    integerCell(entry.year),
                    dateCell(entry.weekStartDate),
                    dateCell(entry.weekEndDate),
                    moneyCell(entry.totalAmount),
                    optionalDecimal2(
                        entry.gallons?.let { gallons ->
                            if (input.metric) MeasurementUnits.displayVolume(gallons, true) else gallons
                        },
                    ),
                    optionalUnitPrice(entry.pricePerGallon, input.metric),
                    optionalUnitPrice(entry.discountPricePerGallon, input.metric),
                    optionalMoney(entry.savingsAmount),
                    text(entry.location.orEmpty()),
                )
            }
        return worksheet("Дизель", listOf(header) + body, headerRow = true)
    }

    private fun perDiemSheet(input: Input): String {
        val header = listOf(text("Дата на рейсе"), text("Ставка, USD"), text("Сумма, USD"))
        val rate = PerDiemCalculator.DAILY_RATE
        val body = input.perDiemDates.sorted().map { date ->
            listOf(dateCell(date), moneyCell(rate), moneyCell(rate))
        }
        val totalRow = listOf(
            text("ИТОГО (${input.perDiemDates.size} дн.)"),
            moneyCell(rate),
            moneyCell(PerDiemCalculator.amount(input.perDiemDates.size)),
        )
        return worksheet("Суточные", listOf(header) + body + listOf(totalRow), headerRow = true)
    }

    private fun maintenanceSheet(input: Input): String {
        val header = listOf(text("Дата ТО"), text("Услуга"), text("Описание"), text("Сумма, USD"))
        val body = input.maintenance.map { entry ->
            listOf(
                dateCell(entry.serviceDate),
                text(entry.serviceName),
                text(entry.description),
                moneyCell(entry.amount),
            )
        }
        val total = listOf(
            text("ИТОГО"),
            blank(),
            blank(),
            moneyCell(input.maintenance.map { it.amount }.sum()),
        )
        return worksheet("ТО", listOf(header) + body + listOf(total), headerRow = true)
    }

    private fun worksheet(name: String, rows: List<List<Cell>>, headerRow: Boolean): String {
        val safeName = name.take(31).replace(Regex("""[\\/*?:\[\]]"""), "-")
        return buildString {
            appendLine("""  <Worksheet ss:Name="${xmlEscape(safeName)}">""")
            appendLine("    <Table>")
            rows.forEachIndexed { index, row ->
                val rowStyle = if (headerRow && index == 0) """ ss:StyleID="Header"""" else ""
                appendLine("      <Row$rowStyle>")
                row.forEach { cell -> appendLine(renderCell(cell)) }
                appendLine("      </Row>")
            }
            appendLine("    </Table>")
            appendLine("  </Worksheet>")
        }
    }

    private fun renderCell(cell: Cell): String {
        val style = cell.styleId?.let { """ ss:StyleID="$it"""" }.orEmpty()
        return """        <Cell$style><Data ss:Type="${cell.type}">${xmlEscape(cell.value)}</Data></Cell>"""
    }

    private fun text(value: String): Cell = Cell(value)

    private fun blank(): Cell = text("")

    private fun number(value: String, styleId: String): Cell =
        Cell(value = value, type = TYPE_NUMBER, styleId = styleId)

    private fun moneyCell(value: Money): Cell = number(value.formatPlain(), STYLE_MONEY)

    private fun moneyCell(value: Double): Cell = number(formatDecimal2(value), STYLE_MONEY)

    private fun milesCell(value: Double): Cell = number(formatDecimal0(value), STYLE_MILES)

    private fun decimal2Cell(value: Double): Cell = number(formatDecimal2(value), STYLE_DECIMAL2)

    private fun priceCell(value: UnitPrice): Cell = number(value.formatPlain(), STYLE_PRICE)

    private fun integerCell(value: Int): Cell = number(value.toString(), STYLE_INTEGER)

    private fun optionalMoney(value: Money?): Cell = value?.let { moneyCell(it) } ?: blank()

    private fun optionalPrice(value: UnitPrice?): Cell = value?.let { priceCell(it) } ?: blank()

    private fun optionalUnitPrice(value: UnitPrice?, metric: Boolean): Cell {
        if (value == null) return blank()
        if (!metric) return priceCell(value)
        val shown = MeasurementUnits.displayPricePerVolume(value.thousandths / 1000.0, true)
        return number(String.format(Locale.ROOT, "%.3f", shown), STYLE_PRICE)
    }

    private fun optionalDecimal2(value: Double?): Cell = value?.let { decimal2Cell(it) } ?: blank()

    private fun dateCell(iso: String): Cell {
        val day = iso.trim()
        if (day.isEmpty()) return blank()
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return text(iso)
        return Cell(value = "${parsed}T00:00:00.000", type = TYPE_DATE, styleId = STYLE_DATE)
    }

    /** Invariant dot decimal, no grouping, independent of the JVM default locale. */
    private fun formatDecimal0(value: Double): String = String.format(Locale.ROOT, "%.0f", value)

    private fun formatDecimal2(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

    fun xmlEscape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private data class Cell(
        val value: String,
        val type: String = TYPE_STRING,
        val styleId: String? = null,
    )

    private const val TYPE_STRING = "String"
    private const val TYPE_NUMBER = "Number"
    private const val TYPE_DATE = "DateTime"
    private const val STYLE_MONEY = "Money"
    private const val STYLE_MILES = "Miles"
    private const val STYLE_DECIMAL2 = "Decimal2"
    private const val STYLE_PRICE = "PricePerGallon"
    private const val STYLE_INTEGER = "Integer"
    private const val STYLE_DATE = "Date"

    private const val WORKBOOK_STYLES = """  <Styles>
    <Style ss:ID="Header">
      <Font ss:Bold="1"/>
      <Interior ss:Color="#E6EDE9" ss:Pattern="Solid"/>
    </Style>
    <Style ss:ID="Money">
      <NumberFormat ss:Format="#,##0.00"/>
    </Style>
    <Style ss:ID="Miles">
      <NumberFormat ss:Format="0"/>
    </Style>
    <Style ss:ID="Decimal2">
      <NumberFormat ss:Format="0.00"/>
    </Style>
    <Style ss:ID="PricePerGallon">
      <NumberFormat ss:Format="0.000"/>
    </Style>
    <Style ss:ID="Integer">
      <NumberFormat ss:Format="0"/>
    </Style>
    <Style ss:ID="Date">
      <NumberFormat ss:Format="yyyy\-mm\-dd"/>
    </Style>
  </Styles>"""
}
