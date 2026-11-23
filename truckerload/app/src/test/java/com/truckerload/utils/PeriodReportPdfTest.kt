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
        assertTrue(text.contains("$1,971.00"))
        assertTrue(text.contains("$891.00"))
        assertTrue(text.contains("$1,080.00"))
        assertTrue(text.contains("Nov 15, 2026"))
        assertTrue(text.contains("11/16/26"))
        assertTrue(text.contains("Stored only on this device until you share it."))
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
    }

    @Test
    fun pdf_staysEnglishWhenTheAppLocaleIsRussian() {
        val russianNumbers = PeriodReportNumbers(
            money = { "$${it.formatPlain()}" },
            miles = { "${it.toInt()} км" },
            rpm = { String.format(Locale.US, "$%.2f/км", it) },
            volume = { "${it.toInt()} л" },
            date = { it.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))) },
        )
        val report = PeriodReportBuilder.build(
            sampleDraft().copy(
                locale = Locale.forLanguageTag("ru"),
                periodLabel = "Неделя 47 · 15 нояб. 2026 – 21 нояб. 2026",
            ),
            russianPlainLabels(),
            russianNumbers,
        )
        val (regular, bold) = fonts()
        val text = PdfTextLayerExtractor.extract(PeriodReportPdf.render(report, regular, bold))
        assertTrue(text.contains("Load Report"))
        assertTrue(text.contains("Origin"))
        assertTrue(text.contains("Ship date"))
        assertTrue(text.contains("Delivery"))
        assertTrue(text.contains("Total"))
        assertTrue(text.contains("Summary"))
        assertTrue(text.contains("Loads total"))
        assertTrue(text.contains("Report date: Nov 21, 2026"))
        assertTrue(text.contains("Period: Week 47 · Nov 15, 2026 – Nov 21, 2026"))
        assertTrue(text.contains("$1,971.00"))
        assertTrue(text.contains("$2.69/mi"))
        assertTrue(text.contains("Stored only on this device until you share it."))
        assertFalse(text.contains("Отчёт"))
        assertFalse(text.contains("Откуда"))
        assertFalse(text.contains("Итого"))
        assertFalse(text.contains("нояб"))
        assertFalse(text.contains("/км"))
        assertTrue(report.plainText.contains("Отчёт за период"))
        assertTrue(report.plainText.contains("Водитель: Alex Morgan"))
        assertTrue(report.plainText.contains("Неделя 47"))
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
            statementRows = emptyList(),
            emptyLoads = "За этот период грузов нет.",
            footer = "Хранится только на этом устройстве",
            plainText = "",
        )
        val (regular, bold) = fonts()
        val text = PdfTextLayerExtractor.extract(PeriodReportPdf.render(empty, regular, bold))
        assertTrue(text.contains("No loads in this period."))
        assertTrue(text.contains("Load Report"))
        assertTrue(text.contains("Stored only on this device until you share it."))
        assertFalse(text.contains("За этот"))
        assertFalse(text.contains("Хранится"))
    }

    private fun assertEqualsPdf(bytes: ByteArray) {
        val header = String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)
        assertTrue(header, header == "%PDF-")
    }

    private fun sampleReport(): PeriodReport = PeriodReportBuilder.build(
        sampleDraft(),
        englishLabels(),
        sampleNumbers(),
    )

    private fun sampleNumbers() = PeriodReportNumbers(
        money = { "$${it.formatPlain()}" },
        miles = { "${it.toInt()} mi" },
        rpm = { String.format(Locale.US, "$%.2f/mi", it) },
        volume = { "${it.toInt()} gal" },
        date = { it.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) },
    )

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

    private fun sampleDraft() = PeriodReportDraft(
        driverName = "Alex Morgan",
        companyName = "Northline Freight",
        periodLabel = "Week 47 · Nov 15, 2026 – Nov 21, 2026",
        periodStart = LocalDate.of(2026, 11, 15),
        periodEnd = LocalDate.of(2026, 11, 21),
        weekNumber = 47,
        generatedOn = LocalDate.of(2026, 11, 21),
        locale = Locale.US,
        loads = listOf(
            draft("b", "2026-11-18", "DEMO-1002", "Denver, CO", "Omaha, NE", 420.0, 0.0, 1080.0, "Nov 18, 2026"),
            draft("a", "2026-11-16", "DEMO-1001", "Boise, ID", "Denver, CO", 300.0, 31.0, 891.0, "Nov 16, 2026"),
        ),
        fuel = Money.fromDollars(180.0),
        fuelGallons = 48.0,
        showFuel = true,
        expenses = Money.fromDollars(25.0),
        showExpenses = true,
        goal = Money.fromDollars(12_000.0),
    )

    private fun russianPlainLabels(): PeriodReportLabels {
        val english = englishLabels()
        return english.copy(
            title = "Отчёт за период",
            driver = "Водитель",
            period = "Период",
            generated = "Сформирован",
            summary = "Итоги",
            footer = "Хранится только на этом устройстве, пока вы сами не отправите файл.",
            emptyLoads = "За этот период грузов нет.",
            pdf = PeriodReportPdfLabels(
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
            ),
        )
    }

    private fun englishLabels() = PeriodReportLabels(
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
