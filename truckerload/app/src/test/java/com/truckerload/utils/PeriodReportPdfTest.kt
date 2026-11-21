package com.truckerload.utils

import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.money.Money
import com.truckerload.domain.report.PeriodReport
import com.truckerload.domain.report.PeriodReportBuilder
import com.truckerload.domain.report.PeriodReportDraft
import com.truckerload.domain.report.PeriodReportLabels
import com.truckerload.domain.report.PeriodReportLoadDraft
import com.truckerload.domain.report.PeriodReportNumbers
import com.truckerload.domain.report.ReportLine
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class PeriodReportPdfTest {

    @Test
    fun pdf_embedsTextInContractOrder_includingCyrillic() {
        val report = sampleReport()
        val (regular, bold) = fonts()
        val bytes = PeriodReportPdf.render(report, regular, bold)
        assertTrue(bytes.size > 4)
        assertEqualsPdf(bytes)
        val text = PdfTextLayerExtractor.extract(bytes)
        assertBefore(text, "TruckoRig", "Period report")
        assertBefore(text, "Driver", "Khaidar Safarov")
        assertBefore(text, "Summary", "Gross")
        assertBefore(text, "T-116KYL6KW", "T-220")
        assertTrue(text.contains("Garner, NC"))
        assertTrue(text.contains("Dallas, TX"))
        val outDir = File("build/period-report-sample")
        outDir.mkdirs()
        File(outDir, "period-report-en.pdf").writeBytes(bytes)
        File(outDir, "period-report-en.txt").writeText(report.plainText)
        val russian = report.copy(
            title = "Отчёт за период",
            header = listOf(
                ReportLine("Водитель", "Хайдар Сафаров"),
                ReportLine("Период", "Неделя 47 · 15 нояб. 2026 – 21 нояб. 2026"),
                ReportLine("Сформирован", "21 нояб. 2026"),
            ),
            summaryTitle = "Итоги",
            plainText = report.plainText
                .replace("Period report", "Отчёт за период")
                .replace("Driver:", "Водитель:")
                .replace("Khaidar Safarov", "Хайдар Сафаров"),
        )
        val russianBytes = PeriodReportPdf.render(russian, regular, bold)
        val russianText = PdfTextLayerExtractor.extract(russianBytes)
        assertTrue(russianText.contains("Водитель"))
        assertTrue(russianText.contains("Хайдар Сафаров"))
        assertTrue(russianText.contains("Итоги"))
        File(outDir, "period-report-ru.txt").writeText(russian.plainText)
        File(outDir, "period-report-ru.pdf").writeBytes(russianBytes)
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
            draft("b", "2026-11-18", "T-220", "Charlotte, NC", "Atlanta, GA", 420.0, 0.0, 1080.0, "Nov 18, 2026"),
            draft("a", "2026-11-16", "T-116KYL6KW", "Garner, NC", "Dallas, TX", 300.0, 31.0, 891.0, "Nov 16, 2026"),
        )
        return PeriodReportBuilder.build(
            PeriodReportDraft(
                driverName = "Khaidar Safarov",
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
            loadedMiles = loaded,
            emptyMiles = empty,
            rate = money,
            rpm = money.cents / 100.0 / driven,
            equipment = if (trip.startsWith("T-116")) "Dry Van" else "",
            status = "",
            notes = if (trip.startsWith("T-220")) "Lumper paid" else "",
            penalties = Money.ZERO,
        )
    }

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
