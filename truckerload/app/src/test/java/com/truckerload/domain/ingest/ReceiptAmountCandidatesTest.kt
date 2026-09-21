package com.truckerload.domain.ingest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAmountCandidatesTest {

    @Test
    fun paycheck_primex_offersGrandTotalAndGrossNotLineNets() {
        val text = """
            Driver Settlement
            Grand Total ${'$'}4,040.30
            Gross Pay Total: ${'$'}8,454.58
            T-113RD815D Indianapolis 88% ${'$'}3,278.50
            Total Deductions ${'$'}3,399.73
        """.trimIndent()

        val choices = ReceiptAmountCandidates.paycheck(text)
        val amounts = choices.map { it.amount }

        assertTrue(amounts.any { kotlin.math.abs(it - 4040.30) < 0.01 })
        assertTrue(amounts.any { kotlin.math.abs(it - 8454.58) < 0.01 })
        assertTrue(amounts.none { kotlin.math.abs(it - 3278.50) < 0.01 })
        assertTrue(amounts.none { kotlin.math.abs(it - 3399.73) < 0.01 })
        assertEquals(4040.30, choices.first().amount, 0.01)
    }

    @Test
    fun diesel_offersPumpTotal() {
        val text = """
            PILOT
            DIESEL #2
            GALLONS 120.543
            PRICE/GAL ${'$'}3.899
            PUMP TOTAL ${'$'}469.99
        """.trimIndent()
        val choices = ReceiptAmountCandidates.diesel(text)
        assertTrue(choices.any { kotlin.math.abs(it.amount - 469.99) < 0.01 })
    }

    @Test
    fun parseTypedAmount_acceptsPlainAndDollar() {
        assertEquals(4040.30, ReceiptAmountCandidates.parseTypedAmount("4040.30")!!, 0.01)
        assertEquals(4040.30, ReceiptAmountCandidates.parseTypedAmount("${'$'}4,040.30")!!, 0.01)
        assertEquals(2100.0, ReceiptAmountCandidates.parseTypedAmount("Grand Total 2100.00")!!, 0.01)
    }

    @Test
    fun parseTypedAmount_rejectsLoadAndLongPaste() {
        assertNull(ReceiptAmountCandidates.parseTypedAmount("Trip ID: T-116KYL6KW\nTotal Rate: 2500"))
        assertNull(ReceiptAmountCandidates.parseTypedAmount("   "))
    }
}
