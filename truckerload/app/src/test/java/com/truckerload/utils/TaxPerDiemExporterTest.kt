package com.truckerload.utils

import com.truckerload.domain.tax.PerDiemCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxPerDiemExporterTest {

    @Test
    fun buildCsv_includesSummaryAndSortedDays() {
        val csv = TaxPerDiemExporter.buildCsv(
            year = 2026,
            dates = setOf("2026-06-03", "2026-06-01"),
            dailyRate = PerDiemCalculator.DAILY_RATE,
            dieselDeductions = 120.5,
            grossIncome = 50_000.0,
        )
        assertTrue(csv.contains("# per_diem_days,2"))
        assertTrue(csv.contains("# per_diem_total_usd,160.0") || csv.contains("# per_diem_total_usd,160"))
        assertTrue(csv.contains(TaxPerDiemExporter.CSV_HEADER))
        val lines = csv.lines().filter { it.startsWith("2026-") }
        assertEquals(listOf("2026-06-01,80.00,80.00", "2026-06-03,80.00,80.00"), lines)
    }
}
