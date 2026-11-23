package com.truckerload.utils

import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.money.Money
import com.truckerload.domain.report.PeriodReport
import com.truckerload.domain.report.PeriodReportBuilder
import com.truckerload.domain.report.PeriodReportDraft
import com.truckerload.domain.report.PeriodReportLabels
import com.truckerload.domain.report.PeriodReportLoadDraft
import com.truckerload.domain.report.PeriodReportNumbers
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.truckerload.domain.report.PeriodReportPdfLabels
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PeriodReportPdfTest {

    @Test
    fun pdf_isAStatementTable_withoutDeductions_andKeepsPlainText() {
        val report = sampleReport()
        val (regular, bold) = fonts()
        val bytes = PeriodReportPdf.render(report, regular, bold)
        assertEqualsPdf(bytes)
        val text = PdfTextLayerExtractor.extract(bytes)
        assertBefore(text, "Northline Freight", "Alex Morgan")
        assertBefore(text, "Load Report", "DEMO-1001")
        assertBefore(text, "DEMO-1001", "DEMO-1002")
        assertTrue(text.contains("Boise, ID"))
        assertTrue(text.contains("Denver, CO"))
        assertTrue(text.contains("Origin"))
        assertTrue(text.contains("Ship date"))
        assertTrue(text.contains("Total"))
        assertTrue(text.contains("Loads total"))
        assertTrue(text.contains("Gross total"))
        assertTrue(text.contains("Miles total"))
        assertFalse(text.contains("Fuel"))
        assertFalse(text.contains("Expense"))
        assertFalse(text.contains("IFTA"))
        assertFalse(text.contains("Deduction"))
        assertFalse(text.contains("LIABILITY"))
        assertTrue(report.plainText.contains("Fuel:"))
        val outDir = File("build/period-report-sample")
        outDir.mkdirs()
        File(outDir, "load-report-en.pdf").writeBytes(bytes)
        File(outDir, "load-report-en.txt").writeText(report.plainText)
        val russian = report.copy(
            driverName = "Алексей Морозов",
            companyName = "Северная линия",
            pdf = russianLabels(),
        )
        val russianBytes = PeriodReportPdf.render(russian, regular, bold)
        val russianText = PdfTextLayerExtractor.extract(russianBytes)
        assertTrue(russianText.contains("Отчёт по грузам"))
        assertTrue(russianText.contains("Алексей Морозов"))
        assertTrue(russianText.contains("Откуда"))
        assertFalse(russianText.contains("IFTA"))
        File(outDir, "load-report-ru.pdf").writeBytes(russianBytes)
    }

    @Test
    fun longTable_repeatsTheHeaderOnTheNextPage() {
        val base = sampleReport()
        val rows = (1..40).map { index ->
            base.statementRows.first().copy(reference = "DEMO-${1000 + index}")
        }
        val report = base.copy(statementRows = rows, totals = base.totals.copy(loadCount = rows.size))
        val (regular, bold) = fonts()
        val bytes = PeriodReportPdf.render(report, regular, bold)
        PdfDocument(PdfReader(ByteArrayInputStream(bytes))).use { doc ->
            assertTrue("expected a second page", doc.numberOfPages >= 2)
        }
        val text = PdfTextLayerExtractor.extract(bytes)
        assertTrue(text.split("Origin").size - 1 >= 2)
        assertFalse(text.contains("IFTA"))
    }

    @Test
    fun emptyPdf_statesThatThePeriodHasNoLoads() {
        val empty = sampleReport().copy(
            loads = emptyList(),
            emptyLoads = "No loads in this period.",
            plainText = "",
        )
        val (regular, bold) = fonts()
        val text = PdfTextLayerExtractor.extract(PeriodReportPdf.render(empty, regular, bold))
        assertTrue(text.contains("No loads in this period."))
    }

    private fun assertEqualsPdf(bytes: ByteArray) {
        val header = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue(header, header == "%PDF-")
    }

    private fun sampleReport(): PeriodReport {
        val numbers = PeriodReportNumbers(
            money = { "$${it.formatPlain()}" },
            miles = { "${it.toInt()} mi" },
            rpm = { String.format(Locale.US, "$%.2f/mi", it) },
            volume = { "${it.toInt()} gal" },
            date = { it.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) },
        )
        val labels = PeriodReportLabels(
            appName = "TruckoRig",
            title = "Period report",
            driver = "Driver",
            period = "Period",
            generated = "Generated",
            summary = "Summary",
            gross = "Gross",
            loads = "Loads",
            loadedMiles = "Loaded miles",
            emptyMiles = "Empty miles",
            totalMiles = "Total miles",
            avgRate = "Avg rate per mile",
            fuel = "Fuel",
            fuelVolume = "Fuel volume",
            expenses = "Expenses",
            penalties = "Penalties",
            net = "Net",
            goal = "Goal",
            goalJoin = "of",
            loadsSection = "Loads",
            emptyLoads = "No loads in this period.",
            pickup = "Pickup",
            delivery = "Delivery",
            reference = "Reference",
            miles = "Miles",
            milesSplit = "%1\$s loaded, %2\$s empty",
            rate = "Rate",
            ratePerMile = "Rate per mile",
            equipment = "Equipment",
            status = "Status",
            notes = "Notes",
            disputeOpen = "Dispute open",
            disputeClosed = "Dispute closed",
            disputeAmount = "Dispute amount",
            footer = "Stored only on this device until you share it.",
            equipmentNames = mapOf(EquipmentType.DRY_VAN to "Dry Van"),
        )
        val loads = listOf(
            draft("b", "2026-11-18", "DEMO-1002", "Denver, CO", "Omaha, NE", 420.0, 0.0, 1080.0, "Nov 18, 2026"),
            draft("a", "2026-11-16", "DEMO-1001", "Boise, ID", "Denver, CO", 300.0, 31.0, 891.0, "Nov 16, 2026"),
        )
        return PeriodReportBuilder.build(
            PeriodReportDraft(
                driverName = "Alex Morgan",
                companyName = "Northline Freight",
                periodLabel = "Week 47 · Nov 15, 2026 – Nov 21, 2026",
                generatedOn = LocalDate.of(2026, 11, 21),
                locale = Locale.US,
                loads = loads,
                fuel = Money.fromDollars(180.0),
                fuelGallons = 48.0,
                showFuel = true,
                expenses = Money.fromDollars(25.0),
                showExpenses = true,
                goal = Money.fromDollars(12_000.0),
            ),
            labels,
            numbers,
        )
    }

    private fun draft(
        id: String,
        sortDate: String,
        trip: String,
        pickup: String,
        delivery: String,
        loaded: Double,
        empty: Double,
        rate: Double,
        heading: String,
    ): PeriodReportLoadDraft {
        val money = Money.fromDollars(rate)
        val driven = loaded + empty
        return PeriodReportLoadDraft(
            sortDate = sortDate,
            sortMillis = 1L,
            id = id,
            heading = heading,
            pickup = pickup,
            delivery = delivery,
            reference = trip,
            shipOn = LocalDate.parse(sortDate),
            deliverOn = LocalDate.parse(sortDate),
            loadedMiles = loaded,
            emptyMiles = empty,
            rate = money,
            rpm = money.cents / 100.0 / driven,
            equipment = "",
            status = "",
            notes = "",
            penalties = Money.ZERO,
        )
    }

    private fun russianLabels() = PeriodReportPdfLabels(
        title = "Отчёт по грузам",
        period = "Период",
        reportDate = "Дата отчёта",
        loadNumber = "№ груза",
        origin = "Откуда",
        destination = "Куда",
        shipDate = "Погрузка",
        deliveryDate = "Выгрузка",
        miles = "Мили",
        gross = "Выручка",
        ratePerMile = "Ставка/ми",
        total = "Итого",
        summary = "Итоги",
        loadsTotal = "Всего грузов",
        grossTotal = "Выручка всего",
        milesTotal = "Всего миль",
        avgRate = "Ставка за милю",
    )

    private fun fonts(): Pair<ByteArray, ByteArray> {
        var dir: File? = listOf(
            File("src/main/assets/fonts"),
            File("app/src/main/assets/fonts"),
            File("truckerload/app/src/main/assets/fonts"),
        ).firstOrNull { File(it, "LiberationSans-Regular.ttf").isFile }
        var cursor: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir == null && cursor != null) {
            dir = listOf(
                File(cursor, "src/main/assets/fonts"),
                File(cursor, "truckerload/app/src/main/assets/fonts"),
            ).firstOrNull { File(it, "LiberationSans-Regular.ttf").isFile }
            cursor = cursor.parentFile
        }
        check(dir != null) { "Liberation fonts were not copied into assets/fonts" }
        return File(dir, "LiberationSans-Regular.ttf").readBytes() to
            File(dir, "LiberationSans-Bold.ttf").readBytes()
    }

    private fun assertBefore(text: String, earlier: String, later: String) {
        val start = text.indexOf(earlier)
        val end = text.indexOf(later)
        assertTrue("missing '$earlier' in ${text.take(400)}", start >= 0)
        assertTrue("'$earlier' should precede '$later'", start < end)
    }
}
