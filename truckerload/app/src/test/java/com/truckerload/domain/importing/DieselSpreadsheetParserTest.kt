package com.truckerload.domain.importing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselSpreadsheetParserTest {

    @Test
    fun parseFileName_extractsDriverAndDates() {
        val meta = DieselSpreadsheetParser.parseFileName(
            "Khaidar-Safarov_2026-08-17-2026-08-23.xlsx.xlsx",
        )
        requireNotNull(meta)
        assertEquals("Khaidar Safarov", meta.first)
        assertEquals("2026-08-17", meta.second)
        assertEquals("2026-08-23", meta.third)
    }

    @Test
    fun parse_readsFleetStyleWorkbook() {
        val bytes = MinimalXlsxBuilder.build(
            headers = listOf("Date", "Location", "Gallons", "Price / gal", "Amount"),
            rows = listOf(
                listOf("2026-08-18", "Pilot, Garner NC", "52.10", "3.89", "202.67"),
                listOf("2026-08-20", "Love's, Atlanta GA", "48.00", "3.75", "180.00"),
            ),
        )
        val result = DieselSpreadsheetParser.parse(
            bytes = bytes,
            fileName = "Khaidar-Safarov_2026-08-17-2026-08-23.xlsx",
        )
        assertEquals(2, result.fills.size)
        assertEquals("Khaidar Safarov", result.driverName)
        assertEquals(2026, result.year)
        assertEquals(202.67, result.fills[0].totalAmount, 0.01)
        assertEquals(52.10, result.fills[0].gallons!!, 0.01)
        assertEquals("Pilot, Garner NC", result.fills[0].location)
        assertEquals("2026-08-18", result.fills[0].transactionDate)
    }

    @Test
    fun parse_skipsNonDieselProductRows() {
        val bytes = MinimalXlsxBuilder.build(
            headers = listOf("Date", "Product", "Gallons", "Amount"),
            rows = listOf(
                listOf("2026-08-18", "Diesel #2", "40.0", "150.00"),
                listOf("2026-08-19", "DEF", "5.0", "20.00"),
            ),
        )
        val result = DieselSpreadsheetParser.parse(bytes, "driver_2026-08-17-2026-08-23.xlsx")
        assertEquals(1, result.fills.size)
        assertEquals(150.0, result.fills[0].totalAmount, 0.01)
    }

    @Test
    fun xlsxReader_parsesSharedStrings() {
        val bytes = MinimalXlsxBuilder.build(
            headers = listOf("Date", "Amount"),
            rows = listOf(listOf("2026-08-18", "99.00")),
        )
        val table = XlsxWorkbookReader.readPrimaryTable(bytes)
        assertFalse(table.isEmpty())
        assertEquals("Date", table[0][0])
        assertEquals("99.00", table[1][1])
    }
}
