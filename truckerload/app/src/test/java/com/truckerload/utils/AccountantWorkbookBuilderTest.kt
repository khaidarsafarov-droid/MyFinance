package com.truckerload.utils

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.money.sum
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.tax.PerDiemCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AccountantWorkbookBuilderTest {

    @Test
    fun buildXml_allSections_hasSeparateSheetsAndTotals() {
        val xml = AccountantWorkbookBuilder.buildXml(
            input = sampleInput(),
            requested = setOf(AccountantExportSection.ALL),
        )
        assertTrue(xml.contains("""ss:Name="Сводка""""))
        assertTrue(xml.contains("""ss:Name="Грузы""""))
        assertTrue(xml.contains("""ss:Name="Дизель""""))
        assertTrue(xml.contains("""ss:Name="Суточные""""))
        assertTrue(xml.contains("""ss:Name="ТО""""))
        assertTrue(xml.contains("T-100"))
        assertTrue(xml.contains("Oil change"))
        assertTrue(xml.contains("2026-06-01T00:00:00.000"))
        assertTrue(
            xml.contains(
                """<Data ss:Type="Number">${
                    String.format(Locale.ROOT, "%.2f", PerDiemCalculator.DAILY_RATE)
                }</Data>""",
            ),
        )
        assertTrue(xml.contains("ИТОГО"))
    }

    @Test
    fun buildXml_dieselOnly_omitsOtherDataSheets() {
        val xml = AccountantWorkbookBuilder.buildXml(
            input = sampleInput(),
            requested = setOf(AccountantExportSection.DIESEL),
        )
        assertTrue(xml.contains("""ss:Name="Сводка""""))
        assertTrue(xml.contains("""ss:Name="Дизель""""))
        assertFalse(xml.contains("""ss:Name="Грузы""""))
        assertFalse(xml.contains("""ss:Name="Суточные""""))
        assertFalse(xml.contains("""ss:Name="ТО""""))
        assertTrue(xml.contains("""ss:StyleID="Money"><Data ss:Type="Number">120.50</Data>"""))
    }

    @Test
    fun xmlEscape_escapesSpecialCharacters() {
        assertTrue(
            AccountantWorkbookBuilder.xmlEscape("""A & B <C> "q"""").contains("&amp;"),
        )
        assertTrue(
            AccountantWorkbookBuilder.xmlEscape("""A & B <C> "q"""").contains("&lt;"),
        )
    }

    @Test
    fun fileLabel_reflectsSelection() {
        assertTrue(
            AccountantWorkbookBuilder.fileLabel(setOf(AccountantExportSection.ALL)) == "All",
        )
        assertTrue(
            AccountantWorkbookBuilder.fileLabel(setOf(AccountantExportSection.DIESEL)) == "Diesel",
        )
    }

    @Test
    fun buildXml_typedCells_keepSheetsColumnsAndInvariantNumbersUnderRuLocale() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        try {
            val input = sampleInput().copy(locale = Locale.forLanguageTag("ru-RU"))
            val xml = AccountantWorkbookBuilder.buildXml(
                input = input,
                requested = setOf(AccountantExportSection.ALL),
            )
            assertStyleFormats(xml)
            assertSheetOrder(xml)
            assertInvariantNumbers(xml)
            assertSummary(xml, input)
            assertLoads(xml, input)
            assertDiesel(xml, input)
            assertPerDiem(xml, input)
            assertMaintenance(xml, input)
            assertFalse(xml.contains("2500,00"))
            assertFalse(xml.contains("120,50"))
            assertFalse(xml.contains("40,00"))
            assertFalse(xml.contains("69,00"))
            assertFalse(xml.contains("3,500"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun buildXml_missingValuesStayEmpty_andRealZerosStayNumeric() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ru-RU"))
        try {
            val input = sampleInput().copy(
                locale = Locale.forLanguageTag("ru-RU"),
                loads = listOf(sampleLoad().copy(totalMiles = 0.0, totalRate = Money.fromDollars(100.0))),
                diesel = listOf(
                    sampleDiesel().copy(
                        gallons = null,
                        pricePerGallon = null,
                        discountPricePerGallon = null,
                        location = null,
                        weekStartDate = "",
                        weekEndDate = "   ",
                    ),
                    sampleDiesel().copy(
                        id = 2,
                        weekNumber = 23,
                        gallons = 0.0,
                        pricePerGallon = UnitPrice.ZERO,
                        discountPricePerGallon = null,
                        location = "",
                    ),
                ),
                perDiemDates = emptySet(),
                maintenance = emptyList(),
            )
            val xml = AccountantWorkbookBuilder.buildXml(
                input = input,
                requested = setOf(AccountantExportSection.ALL),
            )

            val loadRow = sheetRows(xml, "Грузы")[1]
            assertEquals(number("0", "Miles"), loadRow[4])
            assertEquals(number("0.00", "Decimal2"), loadRow[5])

            val dieselRows = sheetRows(xml, "Дизель")
            val missing = dieselRows[1]
            assertEquals(blank(), missing[2])
            assertEquals(blank(), missing[3])
            assertEquals(blank(), missing[5])
            assertEquals(blank(), missing[6])
            assertEquals(blank(), missing[7])
            assertEquals(blank(), missing[8])
            assertEquals(blank(), missing[9])
            assertFalse(missing.any { it.type == "Number" && it.value == "0" && it.style != "Integer" })

            val presentZero = dieselRows[2]
            assertEquals(number("0.00", "Decimal2"), presentZero[5])
            assertEquals(number("0.000", "PricePerGallon"), presentZero[6])
            assertEquals(blank(), presentZero[7])
            assertEquals(blank(), presentZero[8])

            val perDiem = sheetRows(xml, "Суточные")
            assertEquals("ИТОГО (0 дн.)", perDiem[1][0].value)
            assertEquals("String", perDiem[1][0].type)
            assertEquals(number("69.00", "Money"), perDiem[1][1])
            assertEquals(number("0.00", "Money"), perDiem[1][2])

            val maintenance = sheetRows(xml, "ТО")
            assertEquals("ИТОГО", maintenance[1][0].value)
            assertEquals(blank(), maintenance[1][1])
            assertEquals(blank(), maintenance[1][2])
            assertEquals(number("0.00", "Money"), maintenance[1][3])
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun buildXml_textCellsStayStringsAndEscape() {
        val load = sampleLoad().copy(tripId = """A&B<C> "q"""")
        val xml = AccountantWorkbookBuilder.buildXml(
            input = sampleInput().copy(loads = listOf(load)),
            requested = setOf(AccountantExportSection.LOADS),
        )
        val row = sheetRows(xml, "Грузы")[1]
        assertEquals("String", row[1].type)
        assertNull(row[1].style)
        assertEquals("A&amp;B&lt;C&gt; &quot;q&quot;", row[1].value)
        assertEquals("String", row[2].type)
    }

    private fun assertStyleFormats(xml: String) {
        val formats = Regex(
            """<Style ss:ID="([^"]+)">\s*<NumberFormat ss:Format="([^"]+)"/>""",
        ).findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }
        assertEquals("#,##0.00", formats["Money"])
        assertEquals("0", formats["Miles"])
        assertEquals("0.00", formats["Decimal2"])
        assertEquals("0.000", formats["PricePerGallon"])
        assertEquals("0", formats["Integer"])
        assertEquals("""yyyy\-mm\-dd""", formats["Date"])
        assertTrue(xml.contains("""ss:ID="Header""""))
    }

    private fun assertSheetOrder(xml: String) {
        val names = listOf("Сводка", "Грузы", "Дизель", "Суточные", "ТО")
        val indexes = names.map { name ->
            val index = xml.indexOf("""ss:Name="$name"""")
            assertTrue("missing sheet $name", index >= 0)
            index
        }
        assertEquals(indexes, indexes.sorted())
    }

    private fun assertInvariantNumbers(xml: String) {
        val numbers = Regex("""<Data ss:Type="Number">([^<]*)</Data>""").findAll(xml).toList()
        assertTrue(numbers.isNotEmpty())
        numbers.forEach { match ->
            assertTrue(
                "number ${match.groupValues[1]} is not invariant",
                match.groupValues[1].matches(Regex("""-?\d+(\.\d+)?""")),
            )
        }
        Regex("""<Data ss:Type="DateTime">([^<]*)</Data>""").findAll(xml).forEach { match ->
            assertTrue(
                match.groupValues[1].matches(Regex("""\d{4}-\d{2}-\d{2}T00:00:00\.000""")),
            )
        }
    }

    private fun assertSummary(xml: String, input: AccountantWorkbookBuilder.Input) {
        val rows = sheetRows(xml, "Сводка")
        val byLabel = rows.associate { it[0].value to it.getOrNull(1) }
        assertEquals(blank(), byLabel.getValue(""))
        assertEquals("String", rows[0][0].type)
        assertEquals(number(input.year.toString(), "Integer"), byLabel.getValue("Год"))
        assertEquals(number("50000.00", "Money"), byLabel.getValue("Валовой доход (paycheck), USD"))
        assertEquals(number("1", "Integer"), byLabel.getValue("Грузов"))
        assertEquals(
            number(input.loads.map { it.totalRate }.sum().formatPlain(), "Money"),
            byLabel.getValue("Сумма ставок грузов, USD"),
        )
        assertEquals(number("1", "Integer"), byLabel.getValue("Заправок дизеля"))
        assertEquals(
            number(input.diesel.map { it.totalAmount }.sum().formatPlain(), "Money"),
            byLabel.getValue("Дизель всего, USD"),
        )
        assertEquals(number("2", "Integer"), byLabel.getValue("Дней на рейсах (суточные)"))
        assertEquals(number("69.00", "Money"), byLabel.getValue("Ставка суточных, USD/день"))
        assertEquals(
            number(String.format(Locale.ROOT, "%.2f", PerDiemCalculator.amount(2)), "Money"),
            byLabel.getValue("Суточные всего, USD"),
        )
        assertEquals(number("1", "Integer"), byLabel.getValue("Записей ТО"))
        assertEquals(
            number(input.maintenance.map { it.amount }.sum().formatPlain(), "Money"),
            byLabel.getValue("ТО всего, USD"),
        )
        assertEquals("String", byLabel.getValue("Подсказка")?.type)
        assertTrue(rows.none { it[0].value == "ИТОГО" })
    }

    private fun assertLoads(xml: String, input: AccountantWorkbookBuilder.Input) {
        val rows = sheetRows(xml, "Грузы")
        assertEquals(
            listOf("Дата", "Trip ID", "Маршрут", "Ставка, USD", "Мили", "RPM", "Дней на рейсе"),
            rows[0].map { it.value },
        )
        assertTrue(rows[0].all { it.type == "String" && it.style == null })
        val metrics = input.loads.single().withRouteMetrics()
        val miles = metrics.totalMiles
        val rpm = metrics.totalRate.cents / 100.0 / miles
        val days = PerDiemCalculator.uniqueOnDutyDates(input.loads, input.year).size
        assertEquals(
            listOf(
                date("2026-06-01"),
                text("T-100"),
                text(formatLoadRoute(metrics)),
                number("2500.00", "Money"),
                number(String.format(Locale.ROOT, "%.0f", miles), "Miles"),
                number(String.format(Locale.ROOT, "%.2f", rpm), "Decimal2"),
                number(days.toString(), "Integer"),
            ),
            rows[1],
        )
    }

    private fun assertDiesel(xml: String, input: AccountantWorkbookBuilder.Input) {
        val rows = sheetRows(xml, "Дизель")
        assertEquals(
            listOf(
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
            ),
            rows[0].map { it.value },
        )
        assertTrue(rows[0].all { it.type == "String" && it.style == null })
        val entry = input.diesel.single()
        assertEquals(
            listOf(
                number("22", "Integer"),
                number("2026", "Integer"),
                date("2026-05-25"),
                date("2026-05-31"),
                number(entry.totalAmount.formatPlain(), "Money"),
                number(String.format(Locale.ROOT, "%.2f", entry.gallons!!), "Decimal2"),
                number(entry.pricePerGallon!!.formatPlain(), "PricePerGallon"),
                blank(),
                blank(),
                text("Pilot"),
            ),
            rows[1],
        )
        assertNull(entry.savingsAmount)
    }

    private fun assertPerDiem(xml: String, input: AccountantWorkbookBuilder.Input) {
        val rows = sheetRows(xml, "Суточные")
        assertEquals(listOf("Дата на рейсе", "Ставка, USD", "Сумма, USD"), rows[0].map { it.value })
        assertEquals(
            listOf(date("2026-06-01"), number("69.00", "Money"), number("69.00", "Money")),
            rows[1],
        )
        assertEquals(
            listOf(date("2026-06-02"), number("69.00", "Money"), number("69.00", "Money")),
            rows[2],
        )
        val total = String.format(
            Locale.ROOT,
            "%.2f",
            PerDiemCalculator.amount(input.perDiemDates.size),
        )
        assertEquals(
            listOf(
                text("ИТОГО (${input.perDiemDates.size} дн.)"),
                number("69.00", "Money"),
                number(total, "Money"),
            ),
            rows[3],
        )
    }

    private fun assertMaintenance(xml: String, input: AccountantWorkbookBuilder.Input) {
        val rows = sheetRows(xml, "ТО")
        assertEquals(listOf("Дата ТО", "Услуга", "Описание", "Сумма, USD"), rows[0].map { it.value })
        val entry = input.maintenance.single()
        assertEquals(
            listOf(
                date("2026-03-10"),
                text("Oil change"),
                text("Full service"),
                number(entry.amount.formatPlain(), "Money"),
            ),
            rows[1],
        )
        assertEquals(
            listOf(
                text("ИТОГО"),
                blank(),
                blank(),
                number(input.maintenance.map { it.amount }.sum().formatPlain(), "Money"),
            ),
            rows[2],
        )
    }

    private fun sheetRows(xml: String, name: String): List<List<XmlCell>> {
        val start = xml.indexOf("""<Worksheet ss:Name="$name">""")
        assertTrue("missing sheet $name", start >= 0)
        val end = xml.indexOf("</Worksheet>", start)
        val sheet = xml.substring(start, end)
        return rowPattern.findAll(sheet).map { row ->
            cellPattern.findAll(row.groupValues[2]).map { cell ->
                XmlCell(
                    type = cell.groupValues[2],
                    style = cell.groupValues[1].ifBlank { null },
                    value = cell.groupValues[3],
                )
            }.toList()
        }.toList()
    }

    private fun text(value: String) = XmlCell(type = "String", style = null, value = value)

    private fun blank() = text("")

    private fun number(value: String, style: String) =
        XmlCell(type = "Number", style = style, value = value)

    private fun date(iso: String) =
        XmlCell(type = "DateTime", style = "Date", value = "${iso}T00:00:00.000")

    private fun sampleInput() = AccountantWorkbookBuilder.Input(
        year = 2026,
        loads = listOf(sampleLoad()),
        diesel = listOf(sampleDiesel()),
        perDiemDates = setOf("2026-06-01", "2026-06-02"),
        maintenance = listOf(
            MaintenanceArchiveEntry(
                id = 1,
                serviceName = "Oil change",
                serviceDate = "2026-03-10",
                description = "Full service",
                amount = Money.fromDollars(350.0),
            ),
        ),
        grossIncome = 50_000.0,
    )

    private fun sampleLoad() = Load(
        id = "1",
        tripId = "T-100",
        date = "2026-06-01",
        totalRate = Money.fromDollars(2500.0),
        totalMiles = 500.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 22,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        durationDays = 1.0,
    )

    private fun sampleDiesel() = Diesel(
        id = 1,
        weekNumber = 22,
        year = 2026,
        weekLabel = "W22",
        weekStartDate = "2026-05-25",
        weekEndDate = "2026-05-31",
        totalAmount = Money.fromDollars(120.5),
        gallons = 40.0,
        pricePerGallon = UnitPrice.fromDollars(3.5),
        location = "Pilot",
        rawExtractedText = "",
        sourceFileName = null,
        addedAt = 1L,
    )

    private data class XmlCell(
        val type: String,
        val style: String?,
        val value: String,
    )

    private companion object {
        val rowPattern = Regex(
            """<Row(?:\s+ss:StyleID="([^"]*)")?>(.*?)</Row>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val cellPattern = Regex(
            """<Cell(?:\s+ss:StyleID="([^"]*)")?>\s*<Data ss:Type="([^"]+)">([^<]*)</Data>\s*</Cell>""",
        )
    }
}
