package com.truckerload.domain.expense

import com.truckerload.domain.model.MiscExpense
import com.truckerload.utils.MiscExpenseExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiscExpenseFieldsTest {

    @Test
    fun parseAmount_acceptsCommaAndDollar() {
        assertEquals(25.5, MiscExpenseFields.parseAmount("25,50")!!, 0.001)
        assertEquals(80.0, MiscExpenseFields.parseAmount("$ 80")!!, 0.001)
        assertNull(MiscExpenseFields.parseAmount("0"))
        assertNull(MiscExpenseFields.parseAmount("abc"))
    }

    @Test
    fun validate_requiresAmountDescriptionAndDate() {
        assertEquals(
            MiscExpenseFields.Error.AMOUNT,
            MiscExpenseFields.validate("", "Parking", "2026-08-01"),
        )
        assertEquals(
            MiscExpenseFields.Error.DESCRIPTION,
            MiscExpenseFields.validate("10", "  ", "2026-08-01"),
        )
        assertEquals(
            MiscExpenseFields.Error.DATE,
            MiscExpenseFields.validate("10", "Parking", "01.08.2026"),
        )
        assertNull(MiscExpenseFields.validate("10.00", "Parking", "2026-08-01"))
    }

    @Test
    fun csvQuote_escapesCommasAndQuotes() {
        assertEquals("Parking", MiscExpenseFields.csvQuote("Parking"))
        assertEquals("\"Parking, NYC\"", MiscExpenseFields.csvQuote("Parking, NYC"))
        assertEquals("\"He said \"\"ok\"\"\"", MiscExpenseFields.csvQuote("He said \"ok\""))
    }
}

class MiscExpenseExporterTest {

    @Test
    fun buildCsv_sortsAndTotals() {
        val csv = MiscExpenseExporter.buildCsv(
            listOf(
                expense(id = 2, date = "2026-08-03", description = "Scales, NJ", amount = 20.0),
                expense(id = 1, date = "2026-08-01", description = "Parking", amount = 12.5),
            ),
        )
        assertTrue(csv.contains("# count,2"))
        assertTrue(csv.contains("# total_usd,32.50"))
        assertTrue(csv.contains(MiscExpenseExporter.CSV_HEADER))
        val rows = csv.lines().filter { it.startsWith("2026-") }
        assertEquals(
            listOf(
                "2026-08-01,Parking,12.50,no",
                "2026-08-03,\"Scales, NJ\",20.00,no",
            ),
            rows,
        )
    }

    @Test
    fun buildCsv_marksAttachedReceipt() {
        val csv = MiscExpenseExporter.buildCsv(
            listOf(
                expense(id = 1, date = "2026-08-01", description = "Parking", amount = 12.5)
                    .copy(receiptPhotoPath = "/tmp/receipt.jpg"),
            ),
        )
        assertTrue(csv.contains("2026-08-01,Parking,12.50,yes"))
    }

    private fun expense(
        id: Int,
        date: String,
        description: String,
        amount: Double,
    ) = MiscExpense(
        id = id,
        amount = amount,
        description = description,
        date = date,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
