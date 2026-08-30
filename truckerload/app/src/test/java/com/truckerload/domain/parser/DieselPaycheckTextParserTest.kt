package com.truckerload.domain.parser

import com.truckerload.domain.ingest.InboundDocumentResolver
import com.truckerload.domain.ingest.ReceiptFieldExtractor
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptKindClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DieselPaycheckTextParserTest {

    @Test
    fun diesel_parseFullReceipt_extractsAmountsAndLocation() {
        val text = """
            Fuel Receipt
            Date: 07/21/2026
            Merchant: Pilot
            Location: Knoxville, TN
            120.5 gallons
            @ ${'$'}4.15/gal
            Total Amount: ${'$'}500.08
        """.trimIndent()

        val parsed = DieselTextParser.parse(text)!!

        assertEquals("2026-07-21", parsed.date)
        assertEquals(500.08, parsed.totalAmount, 0.01)
        assertEquals(120.5, parsed.gallons!!, 0.01)
        assertEquals(4.15, parsed.pricePerGallon!!, 0.01)
        assertEquals("Knoxville, TN", parsed.location)
    }

    @Test
    fun diesel_parseAmountDueWithoutOptionalFields_stillParsesTotal() {
        val parsed = DieselTextParser.parse("Diesel\nAmount Due: ${'$'}75.50")

        assertEquals(75.50, parsed!!.totalAmount, 0.01)
        assertNull(parsed.gallons)
        assertNull(parsed.pricePerGallon)
    }

    @Test
    fun diesel_rejectsReceiptWithoutPositiveTotal() {
        assertFalse(DieselTextParser.looksLikeDiesel("Fuel receipt\n120 gal"))
        assertNull(DieselTextParser.parse("Fuel receipt\nTotal Amount: ${'$'}0.00"))
    }

    @Test
    fun paycheck_parseSettlement_extractsDriverDatesAndAmounts() {
        val text = """
            Driver: Alex Driver
            Week Start: 07/14/2026
            Week End: 07/20/2026
            Gross Pay: ${'$'}3,400.00
            Net Pay: ${'$'}2,750.25
        """.trimIndent()

        val parsed = PaycheckTextParser.parse(text)!!

        assertEquals("Alex Driver", parsed.driverName)
        assertEquals("2026-07-14", parsed.weekStartDate)
        assertEquals("2026-07-20", parsed.weekEndDate)
        assertEquals(3400.0, parsed.grossAmount!!, 0.01)
        assertEquals(2750.25, parsed.netAmount, 0.01)
    }

    @Test
    fun paycheck_parseGrandTotal_acceptsAlternateNetMarker() {
        val parsed = PaycheckTextParser.parse("Grand Total: ${'$'}1,100.00")

        assertEquals(1100.0, parsed!!.netAmount, 0.01)
        assertEquals("high", parsed.confidence)
    }

    @Test
    fun paycheck_prefersNetPayOverGrandTotal() {
        val parsed = PaycheckTextParser.parse(
            """
            Gross Pay: ${'$'}3,400.00
            Net Pay: ${'$'}2,750.25
            Grand Total: ${'$'}3,400.00
            """.trimIndent(),
        )
        assertEquals(2750.25, parsed!!.netAmount, 0.01)
        assertEquals(3400.0, parsed.grossAmount!!, 0.01)
    }

    @Test
    fun paycheck_rejectsMissingNetAmount() {
        assertFalse(PaycheckTextParser.looksLikePaycheck("Driver Settlement\nGross Pay: ${'$'}900"))
        assertNull(PaycheckTextParser.parse("Net Pay: ${'$'}0.00"))
    }

    @Test
    fun paycheck_looksLikePaycheck_acceptsSettlementTotalMarker() {
        assertTrue(PaycheckTextParser.looksLikePaycheck("Settlement Total: ${'$'}950.00"))
    }

    @Test
    fun paycheck_primexDriverSettlement_usesGrandTotalNotLineNetPay() {
        val text = """
            PRIMEX GROUP LOGISTICS LLC
            3601 Algonquin Rd Ste 214
            Driver Settlement
            Payee ID: 268709
            Settlement Date: 08/26/26
            KHAIDAR SAFAROV
            Load # Origin Destn Ship Date Delv Date Gross Method Net Pay
            T-113RD815D Indianapolis, IN Brookshire, TX 08/17/26 08/19/26 ${'$'}3,725.57 88% ${'$'}3,278.50
            T-114LZPHKC Kyle, TX Hebron, KY ${'$'}3,390.27 88% ${'$'}2,983.44
            111B78L1H Hebron, KY Charlotte, NC ${'$'}1,338.74 88% ${'$'}1,178.09
            Total ${'$'}7,440.03
            IFTA ${'$'}50.00
            FUEL SAMEDAY ${'$'}2,944.73
            Total Deductions ${'$'}3,399.73
            Grand Total ${'$'}4,040.30
            Settlement Summary
            Loads Total: 3
            Gross Pay Total: ${'$'}8,454.58
            Miles Total: 2662.019
        """.trimIndent()

        val parsed = PaycheckTextParser.parse(
            text,
            fileName = "Settlement 08.17-08.23 Khaidar Safarov.pdf",
        )!!
        assertEquals(4040.30, parsed.netAmount, 0.01)
        assertEquals(8454.58, parsed.grossAmount!!, 0.01)
        assertEquals("2026-08-26", parsed.weekStartDate)
        assertTrue(PaycheckTextParser.isDriverStatement(text))
        assertEquals(
            ReceiptKind.PAYCHECK,
            ReceiptKindClassifier.classify(
                text,
                "Settlement 08.17-08.23 Khaidar Safarov.pdf",
            ),
        )
        val preview = ReceiptFieldExtractor.extract(
            text,
            fileName = "Settlement 08.17-08.23 Khaidar Safarov.pdf",
        )
        assertEquals(4040.30, preview.amount!!, 0.01)
        assertFalse(
            InboundDocumentResolver.resolve(
                text,
                fileName = "Settlement 08.17-08.23 Khaidar Safarov.pdf",
            ).autoSaveLoads,
        )
    }

    @Test
    fun paycheck_grandTotalOnNextLine() {
        val parsed = PaycheckTextParser.parse(
            """
            Driver Settlement
            Grand Total
            ${'$'}4,040.30
            """.trimIndent(),
        )
        assertEquals(4040.30, parsed!!.netAmount, 0.01)
    }

    @Test
    fun paycheck_ocrSpacedGrandTotal() {
        val parsed = PaycheckTextParser.parse(
            """
            Driver Settlement
            Grand Tota1
            4 040.30
            """.trimIndent(),
        )
        assertEquals(4040.30, parsed!!.netAmount, 0.01)
    }

    @Test
    fun paycheck_statementWithoutGrandTotal_usesLoadsMinusDeductions() {
        val parsed = PaycheckTextParser.parse(
            """
            Driver Settlement
            Payee ID: 268709
            T-113RD815D Indianapolis IN 88% ${'$'}3,278.50
            T-114LZPHKC Kyle TX 88% ${'$'}2,983.44
            111B78L1H Hebron KY 88% ${'$'}1,178.09
            Total ${'$'}7,440.03
            Total Deductions ${'$'}3,399.73
            """.trimIndent(),
        )
        assertEquals(4040.30, parsed!!.netAmount, 0.01)
    }

    @Test
    fun paycheck_takeHomeAndCheckAmountLabels() {
        assertEquals(
            2100.0,
            PaycheckTextParser.parse("Weekly Settlement\nTake Home: ${'$'}2,100.00")!!.netAmount,
            0.01,
        )
        assertEquals(
            990.5,
            PaycheckTextParser.parse("Pay Statement\nCheck Amount ${'$'}990.50")!!.netAmount,
            0.01,
        )
    }
}
