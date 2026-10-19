package com.truckerload.presentation.screens.diesel

import com.truckerload.domain.importing.DieselImportComparison
import com.truckerload.domain.importing.DieselImportReview
import com.truckerload.domain.importing.DieselSpreadsheetImport
import com.truckerload.domain.importing.DieselSpreadsheetParser
import com.truckerload.domain.importing.MinimalXlsxBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselImportReviewDispatcherTest {

    @Test
    fun parseAndReviewOnDefaultMatchADirectCall() = runBlocking {
        val bytes = workbook()
        val fileName = "Khaidar-Safarov_2026-08-17-2026-08-23.xlsx"
        val direct = reviewOf(DieselSpreadsheetParser.parse(bytes, fileName))
        val offMain = parseDieselReview(bytes, fileName, ::reviewOf)
        assertEquals(direct, offMain)
    }

    @Test
    fun parseAndReviewRunOnTheDefaultDispatcher() = runBlocking {
        var worker = ""
        parseDieselReview(workbook(), "driver_2026-08-17-2026-08-23.xlsx") { import ->
            worker = Thread.currentThread().name
            reviewOf(import)
        }
        assertTrue(worker, worker.contains("DefaultDispatcher"))
    }

    private fun reviewOf(import: DieselSpreadsheetImport): DieselImportReview {
        val importedTotal = import.fills.sumOf { it.totalAmount }
        val importedGallons = import.fills.sumOf { it.gallons ?: 0.0 }
        return DieselImportReview(
            import = import,
            existing = emptyList(),
            importedPreview = emptyList(),
            comparison = DieselImportComparison(
                existingCount = 0,
                importedCount = import.fills.size,
                existingTotal = 0.0,
                importedTotal = importedTotal,
                existingGallons = 0.0,
                importedGallons = importedGallons,
            ),
            hasConflict = false,
        )
    }

    private fun workbook(): ByteArray = MinimalXlsxBuilder.build(
        headers = listOf("Date", "Location", "Gallons", "Price / gal", "Amount"),
        rows = listOf(
            listOf("2026-08-18", "Pilot, Garner NC", "52.10", "3.89", "202.67"),
            listOf("2026-08-20", "Love's, Atlanta GA", "48.00", "3.75", "180.00"),
        ),
    )
}
