package com.truckerload.domain.parser

import com.truckerload.domain.ingest.InboundDocumentResolver
import com.truckerload.domain.ingest.ReceiptAmountCandidates
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
        assertEquals("KHAIDAR SAFAROV", parsed.driverName)
        assertEquals("2026-08-17", parsed.weekStartDate)
        assertEquals("2026-08-19", parsed.weekEndDate)
        assertEquals("2026-08-26", parsed.settlementDate)
        assertEquals(2662.019, parsed.miles!!, 0.001)
        assertEquals(7440.03, parsed.loadsNetAmount!!, 0.01)
        assertEquals(3399.73, parsed.deductionsAmount!!, 0.01)
        assertEquals("high", parsed.confidence)
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
        assertEquals(8454.58, preview.grossAmount!!, 0.01)
        assertEquals("KHAIDAR SAFAROV", preview.driverName)
        assertEquals("2026-08-17", preview.date)
        assertEquals(2662.019, preview.miles!!, 0.001)
        assertNull(preview.tripId)
        assertFalse(
            InboundDocumentResolver.resolve(
                text,
                fileName = "Settlement 08.17-08.23 Khaidar Safarov.pdf",
            ).autoSaveLoads,
        )
    }

    @Test
    fun paycheck_zp7611Layout_keepsTakeHomeAndUsesLoadWeek() {
        val text = """
            PRIMEX GROUP LOGISTICS LLC Driver Settlement
            3601 ALGONQUIN RD STE 214
            Settlement #:
            ROLLING MEADOWS, IL 60008
            Payee ID: 268709
            Settlement Date: 10/07/26
            KHAIDAR SAFAROV
            1 MAIN ST
            PHILADELPHIA, PA 19114
            Load # Origin Destn Ship Date Delv Date Gross Method Net Pay
            88.00
            T-113SFQ84J PITTSTON, PA CARLISLE, PA 09/27/26 09/27/26 ${'$'}672.73 ${'$'}592.00
            percent
            Order Total:
            ${'$'}592.00
            88.00
            T-115RX9TFD LEWISBERRY, PA UXBRIDGE, MA 09/28/26 09/29/26 ${'$'}1,605.05 ${'$'}1,412.44
            percent
            Order Total:
            ${'$'}1,412.44
            88.00
            T-1135WXBFW CHARLTON, MA CARLISLE, PA 09/29/26 09/29/26 ${'$'}1,485.92 ${'$'}1,307.61
            percent
            Order Total: ${'$'}1,307.61
            HAGERSTOWN, HAGERSTOWN, 88.00
            ${'$'}1,508.66
            T-115JWXXPZ 09/30/26 10/01/26 ${'$'}1,714.39
            MD MD percent
            Order Total: ${'$'}1,508.66
            HAGERSTOWN, HAGERSTOWN, 88.00
            ${'$'}1,283.69
            T-116LJZY9Z 10/01/26 10/02/26 ${'$'}1,458.74
            MD MD percent
            Order Total: ${'$'}1,283.69
            HOPEWELL 88.00
            ${'$'}154.00
            114Z7J4J5 BEDFORD, PA 10/02/26 10/02/26 ${'$'}175.00
            JUNCTION, NY percent
            Order Total: ${'$'}154.00
            HOPEWELL 88.00
            ${'$'}890.20
            114GQNPCD BEDFORD, PA 10/03/26 10/03/26 ${'$'}1,011.59
            JUNCTION, NY percent
            Order Total: ${'$'}890.20
            Total: ${'$'}7,148.60
            Deductions
            Description Date Rate
            IFTA 10/05/26 ${'$'}50.00
            LOGBOOK 10/05/26 ${'$'}50.00
            LIABILITY 10/05/26 ${'$'}344.00
            WEEKLY PREPASS ${'$'}6 10/05/26 ${'$'}6.00
            ACH 10/05/26 ${'$'}5.00
            FUEL SAMEDAY 10/05/26 ${'$'}3,513.73
            Total: ${'$'}3,968.73

            Grand Total: ${'$'}3,179.87
            Settlement Summary:
            Loads Total: 7
            Gross Pay Total: ${'$'}8,123.42
            Miles Total: 1508.932
        """.trimIndent()

        val parsed = PaycheckTextParser.parse(text, fileName = "ZP_7611.pdf")!!
        assertEquals(3179.87, parsed.netAmount, 0.01)
        assertEquals(8123.42, parsed.grossAmount!!, 0.01)
        assertEquals("KHAIDAR SAFAROV", parsed.driverName)
        assertEquals("2026-09-27", parsed.weekStartDate)
        assertEquals("2026-10-03", parsed.weekEndDate)
        assertEquals("2026-10-07", parsed.settlementDate)
        assertEquals(1508.932, parsed.miles!!, 0.001)
        assertEquals(7148.60, parsed.loadsNetAmount!!, 0.01)
        assertEquals(3968.73, parsed.deductionsAmount!!, 0.01)
        assertEquals("high", parsed.confidence)

        val preview = ReceiptFieldExtractor.extract(text, fileName = "ZP_7611.pdf")
        assertEquals(ReceiptKind.PAYCHECK, preview.kind)
        assertEquals(3179.87, preview.amount!!, 0.01)
        assertEquals(8123.42, preview.grossAmount!!, 0.01)
        assertNull(preview.tripId)
        assertEquals(1508.932, preview.miles!!, 0.001)
        val choices = ReceiptAmountCandidates.paycheck(text, "ZP_7611.pdf")
        assertEquals(3179.87, choices.first().amount, 0.01)
        assertTrue(choices.any { kotlin.math.abs(it.amount - 8123.42) < 0.01 })
        assertTrue(choices.any { kotlin.math.abs(it.amount - 7148.60) < 0.01 })
        assertTrue(choices.any { kotlin.math.abs(it.amount - 3968.73) < 0.01 })
        assertFalse(
            InboundDocumentResolver.resolve(text, fileName = "ZP_7611.pdf").autoSaveLoads,
        )
    }

    @Test
    fun paycheck_mismatchingTotals_stayLowConfidenceAndKeepPrintedTakeHome() {
        val parsed = PaycheckTextParser.parse(
            """
            Driver Settlement
            Payee ID: 268709
            Settlement Date: 10/07/26
            Grand Total: ${'$'}100.00
            T-113SFQ84J PITTSTON, PA 09/27/26 ${'$'}672.73 88% ${'$'}592.00
            T-115RX9TFD UXBRIDGE, MA 09/29/26 ${'$'}1,605.05 88% ${'$'}1,412.44
            Total: ${'$'}7,148.60
            Total Deductions ${'$'}3,968.73
            Gross Pay Total: ${'$'}8,123.42
            """.trimIndent(),
        )!!
        assertEquals(100.0, parsed.netAmount, 0.01)
        assertEquals("low", parsed.confidence)
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

    @Test
    fun paycheck_amazonStyleNetEarningsAndPayPeriod() {
        val text = """
            Compensation Summary
            Contractor: Alex Driver
            Pay Period: 07/14/2026 - 07/20/2026
            Gross Earnings: ${'$'}3,400.00
            Deductions: ${'$'}649.75
            Net Earnings: ${'$'}2,750.25
        """.trimIndent()

        val parsed = PaycheckTextParser.parse(text)!!
        assertEquals("Alex Driver", parsed.driverName)
        assertEquals("2026-07-14", parsed.weekStartDate)
        assertEquals("2026-07-20", parsed.weekEndDate)
        assertEquals(3400.0, parsed.grossAmount!!, 0.01)
        assertEquals(2750.25, parsed.netAmount, 0.01)
        assertEquals(MessageType.PAYCHECK, MessageClassifier.classify(text))
    }

    @Test
    fun diesel_pilotPumpTotalReceipt() {
        val text = """
            PILOT TRAVEL CENTER
            KNOXVILLE, TN
            07/21/2026 14:32
            PUMP 07
            DIESEL #2
            GALLONS     120.543
            PRICE/GAL   ${'$'}3.899
            PUMP TOTAL  ${'$'}469.99
        """.trimIndent()

        val parsed = DieselTextParser.parse(text)!!
        assertEquals(469.99, parsed.totalAmount, 0.01)
        assertEquals(120.543, parsed.gallons!!, 0.001)
        assertEquals(3.899, parsed.pricePerGallon!!, 0.001)
        assertEquals("2026-07-21", parsed.date)
        assertEquals("Pilot", parsed.vendor)
        assertEquals(MessageType.DIESEL, MessageClassifier.classify(text))
    }

    @Test
    fun diesel_lovesQtyPpuSale() {
        val text = """
            LOVE'S TRAVEL STOP
            Oklahoma City, OK
            ULSD
            QTY 88.210
            PPU 3.459
            SALE ${'$'}305.12
        """.trimIndent()

        val parsed = DieselTextParser.parse(text)!!
        assertEquals(305.12, parsed.totalAmount, 0.01)
        assertEquals(88.210, parsed.gallons!!, 0.001)
        assertEquals(3.459, parsed.pricePerGallon!!, 0.001)
        assertEquals("Love's Travel Stop", parsed.vendor)
    }

    @Test
    fun diesel_fuelCardTransaction() {
        val text = """
            EFS FUEL TRANSACTION
            Merchant: PILOT TRAVEL CENTER
            City: KNOXVILLE, TN
            Product: ULSD
            Quantity: 102.450
            Unit Price: 3.459
            Fuel Amount: ${'$'}354.38
            Date: 07/21/2026
        """.trimIndent()

        val parsed = DieselTextParser.parse(text)!!
        assertEquals(354.38, parsed.totalAmount, 0.01)
        assertEquals(102.450, parsed.gallons!!, 0.001)
        assertEquals(3.459, parsed.pricePerGallon!!, 0.001)
        assertEquals("2026-07-21", parsed.date)
    }

    @Test
    fun diesel_derivesTotalFromGallonsAndPpg() {
        val parsed = DieselTextParser.parse("Diesel\n50 gal\n${'$'}3.99/gal")
        assertEquals(199.50, parsed!!.totalAmount, 0.01)
        assertEquals(50.0, parsed.gallons!!, 0.01)
        assertEquals(3.99, parsed.pricePerGallon!!, 0.01)
    }
}
