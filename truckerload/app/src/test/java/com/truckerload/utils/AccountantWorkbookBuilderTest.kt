package com.truckerload.utils

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.tax.AccountantExportSection
import com.truckerload.domain.tax.PerDiemCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(xml.contains("2026-06-01"))
        assertTrue(xml.contains(String.format("%.2f", PerDiemCalculator.DAILY_RATE)))
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
        assertTrue(xml.contains("120.50") || xml.contains("120.5"))
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

    private fun sampleInput() = AccountantWorkbookBuilder.Input(
        year = 2026,
        loads = listOf(
            Load(
                id = "1",
                tripId = "T-100",
                date = "2026-06-01",
                totalRate = 2500.0,
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
            ),
        ),
        diesel = listOf(
            Diesel(
                id = 1,
                weekNumber = 22,
                year = 2026,
                weekLabel = "W22",
                weekStartDate = "2026-05-25",
                weekEndDate = "2026-05-31",
                totalAmount = 120.5,
                gallons = 40.0,
                pricePerGallon = 3.5,
                location = "Pilot",
                rawExtractedText = "",
                sourceFileName = null,
                addedAt = 1L,
            ),
        ),
        perDiemDates = setOf("2026-06-01", "2026-06-02"),
        maintenance = listOf(
            MaintenanceArchiveEntry(
                id = 1,
                serviceName = "Oil change",
                serviceDate = "2026-03-10",
                description = "Full service",
                amount = 350.0,
            ),
        ),
        grossIncome = 50_000.0,
    )
}
