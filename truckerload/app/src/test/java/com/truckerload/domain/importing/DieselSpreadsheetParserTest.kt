package com.truckerload.domain.importing

import com.truckerload.utils.formatIsoDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import com.truckerload.domain.money.UnitPrice

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
    fun parseDateCell_readsExcelSerial() {
        assertEquals("2026-09-28", DieselSpreadsheetParser.parseDateCell("46293.795138888891"))
        assertEquals("2026-10-04", DieselSpreadsheetParser.parseDateCell("46299.75"))
        assertNull(DieselSpreadsheetParser.parseDateCell("3513.73"))
    }

    @Test
    fun parse_readsPrimexInvoiceWithExcelDatesAndDiscountedAmount() {
        val header = listOf(
            "Driver Name",
            "Tran Date",
            "Location Name",
            "City",
            "State/Prov",
            "Item",
            "Unit Price",
            "Disc PPU",
            "Qty",
            "Disc Amt",
            "Amt",
            "Total Payables after Non-cash Adjustment:",
        )
        val fills = listOf(
            primexRow("46293.795138888891", "PILOT NEWBURGH 394", "NEWBURGH", "NY", "63.9", "6.799", "5.907", "56.9988", "377.4573"),
            primexRow("46294.409722222219", "TA SOUTHINGTON", "MILLDALE", "CT", "142.74", "6.739", "5.848", "127.18134", "834.74352"),
            primexRow("46295.61319444445", "LOVES #682 TRAVEL STOP", "HAGERSTOWN", "MD", "55.99", "6.849", "5.787", "59.46138", "324.01413"),
            primexRow("46296.871527777781", "LOVES #682 TRAVEL STOP", "HAGERSTOWN", "MD", "72.3", "6.849", "5.838", "73.0953", "422.0874"),
            primexRow("46298.714583333334", "LOVES #646 TRAVEL STOP", "WILLINGTON", "CT", "121.63", "6.719", "5.974", "90.61435", "726.61762"),
            primexRow("46299.75", "LOVES #330 TRAVEL STOP", "BAXTER", "TN", "147.58", "5.949", "5.616", "49.14414", "828.80928"),
        )
        val bytes = MinimalXlsxBuilder.build(
            headers = listOf("Driver`s Name", "ULSD Qty", "ULSD Amt", "Total Discount", "Fees", "Total"),
            rows = listOf(
                listOf("Khaidar Safarov", "604.14", "3513.73", "456.50", "0", "3513.73"),
                listOf("DETAILS:"),
                header,
                listOf("Khaidar Safarov"),
                listOf("Khaidar Safarov", "46297", "TA", "CITY", "NY", "DEF", "4.00", "3.50", "5.0", "2.50", "17.50"),
            ) + fills,
        )
        val result = DieselSpreadsheetParser.parse(bytes, "diesel_7299.xlsx")
        assertEquals(6, result.fills.size)
        assertEquals("Khaidar Safarov", result.driverName)
        assertEquals("2026-09-27", result.weekStartDate)
        assertEquals("2026-10-03", result.weekEndDate)
        assertEquals(604.14, result.fills.sumOf { it.gallons ?: 0.0 }, 0.02)
        assertEquals(3513.73, result.fills.sumOf { it.totalAmount }, 0.02)

        val first = result.fills.first()
        assertEquals("2026-09-28", first.transactionDate)
        assertEquals("PILOT NEWBURGH 394, NEWBURGH, NY", first.location)
        assertEquals(63.9, first.gallons!!, 0.01)
        assertEquals(6.799, first.pricePerGallon!!, 0.001)
        assertEquals(5.907, first.discountPricePerGallon!!, 0.001)
        assertEquals(377.4573, first.totalAmount, 0.001)
        assertEquals("2026-10-04", result.fills.last().transactionDate)
        assertEquals("LOVES #330 TRAVEL STOP, BAXTER, TN", result.fills.last().location)

        val useCase = DieselImportUseCase(mock())
        val saved = result.fills.mapIndexed { index, fill -> useCase.toDiesel(result, fill, index + 1) }
        assertEquals(1, saved.map { it.weekNumber to it.year }.distinct().size)
        assertEquals("2026-09-27", saved.first().weekStartDate)
        assertEquals("2026-10-03", saved.first().weekEndDate)
        assertEquals("2026-10-04", formatIsoDate(saved.last().addedAt))
        assertEquals(UnitPrice.fromDollars(5.907), saved.first().discountPricePerGallon!!)

        val preview = com.truckerload.domain.ingest.FleetDieselPreview.from(result)
        requireNotNull(preview)
        val fuelCents = com.truckerload.domain.money.Money.fromDollars(result.fills.sumOf { it.totalAmount })
        assertEquals(com.truckerload.domain.money.Money.parse("3513.73").cents, fuelCents.cents)
        assertEquals(3513.73, preview.amount!!, 0.001)
        assertEquals(604.14, preview.gallons!!, 0.001)
        assertTrue(kotlin.math.abs(preview.amount!! - preview.gallons!!) > 1.0)
        val html = com.truckerload.domain.ingest.ReceiptPreviewFormatter.toHtml(
            preview,
            "Diesel",
            "Save \$3,513.73?",
        )
        assertTrue(html.contains("3,513.73"))
        assertTrue(html.contains("604.14 gal"))
        assertFalse(html.contains("ULSD"))
        assertFalse(html.contains("Total Payables"))
    }

    private fun primexRow(
        serial: String,
        location: String,
        city: String,
        state: String,
        qty: String,
        unitPrice: String,
        discPpu: String,
        discAmt: String,
        amt: String,
    ): List<String> = listOf(
        "Khaidar Safarov",
        serial,
        location,
        city,
        state,
        "ULSD",
        unitPrice,
        discPpu,
        qty,
        discAmt,
        amt,
    )

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
