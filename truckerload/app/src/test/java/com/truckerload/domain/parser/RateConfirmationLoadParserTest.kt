package com.truckerload.domain.parser

import com.truckerload.domain.ingest.InboundDocumentResolver
import com.truckerload.domain.ingest.ReceiptFieldExtractor
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptKindClassifier
import com.truckerload.utils.PdfTextLayerExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class RateConfirmationLoadParserTest {

    @Test
    fun parsesIntegrityExpressRateConfirmation() {
        val load = RateConfirmationLoadParser.parseOne(
            IEL_RATE_CON,
            nowMillis = REF_MILLIS,
            fileName = "2665704-Carrier-Rate-Confirmation.pdf",
        )
        assertNotNull(load)
        assertEquals(2325.84, load!!.totalRate, 0.01)
        assertEquals(742.50, load.totalMiles, 0.01)
        assertTrue(load.tripId.contains("2665704"))
        assertTrue(load.pointA.contains("LAKE ZURICH", ignoreCase = true))
        assertTrue(load.pointB.contains("ELKRIDGE", ignoreCase = true))
        assertEquals("2025-07-25", load.date)
    }

    @Test
    fun ignoresPalletFeeAndLovesTruckStop() {
        val rate = RateConfirmationLoadParser.bestRate(IEL_RATE_CON)
        assertEquals(2325.84, rate!!, 0.01)
        val (pickup, delivery) = RateConfirmationLoadParser.extractStops(IEL_RATE_CON)
        assertFalse(pickup.contains("LOVES", ignoreCase = true))
        assertFalse(delivery.contains("LOVES", ignoreCase = true))
        assertFalse(pickup.contains("Cincinnati", ignoreCase = true))
    }

    @Test
    fun inboundResolverAutoSavesLoadInsteadOfDieselPrompt() {
        val decision = InboundDocumentResolver.resolve(
            text = IEL_RATE_CON,
            fileName = "2665704-Carrier-Rate-Confirmation.pdf",
            referenceMillis = REF_MILLIS,
        )
        assertEquals(ReceiptKind.LOAD, decision.preview.kind)
        assertTrue(decision.autoSaveLoads)
        assertEquals(1, decision.loads.size)
        assertEquals(2325.84, decision.loads.first().totalRate, 0.01)
        assertEquals(2325.84, decision.preview.amount!!, 0.01)
        assertTrue(decision.preview.pointA!!.contains("LAKE ZURICH", ignoreCase = true))
    }

    @Test
    fun filenameRateConfirmationClassifiesAsLoad() {
        assertEquals(
            ReceiptKind.LOAD,
            ReceiptKindClassifier.classify(
                "Estimated Rate: \$1800\nMiles: 400\nPick Ups\nAddress: 1 Main St Houston, TX 77001\nDeliveries\nAddress: 2 Oak Ave Atlanta, GA 30301",
                fileName = "carrier-rate-confirmation.pdf",
            ),
        )
    }

    @Test
    fun parseServiceInboundUsesRateConfirmationFallback() {
        val loads = MessageParseService()
            .parseLoadsFromInboundText(IEL_RATE_CON, REF_MILLIS, "rate-confirmation.pdf")
            .getOrThrow()
        assertEquals(1, loads.size)
        assertEquals(2325.84, loads.first().totalRate, 0.01)
    }

    @Test
    fun rejectsFuelReceipt() {
        assertNull(
            RateConfirmationLoadParser.parseOne(
                """
                Fuel Receipt
                Pilot
                Diesel
                40.0 gallons
                Total Amount: $180.00
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun parsesRealUploadedIelPdfWhenPresent() {
        val pdf = File(
            "/home/ubuntu/.cursor/projects/workspace/uploads/2665704-Carrier-Rate-Confirmation__1___1__c086.pdf",
        )
        assumeTrue("uploaded IEL PDF is present", pdf.exists())
        val text = PdfTextLayerExtractor.extract(pdf.readBytes())
        assertTrue(text.contains("Rate Confirmation", ignoreCase = true))
        val decision = InboundDocumentResolver.resolve(
            text = text,
            fileName = "2665704-Carrier-Rate-Confirmation.pdf",
            referenceMillis = REF_MILLIS,
        )
        assertEquals(ReceiptKind.LOAD, decision.preview.kind)
        assertTrue(decision.autoSaveLoads)
        val load = decision.loads.single()
        assertEquals(2325.84, load.totalRate, 0.01)
        assertEquals(742.50, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("LAKE ZURICH", ignoreCase = true))
        assertTrue(load.pointB.contains("ELKRIDGE", ignoreCase = true))
    }

    @Test
    fun receiptPreviewDoesNotHighlightPalletFee() {
        val preview = ReceiptFieldExtractor.extract(
            IEL_RATE_CON,
            fileName = "2665704-Carrier-Rate-Confirmation.pdf",
        )
        assertEquals(ReceiptKind.LOAD, preview.kind)
        assertEquals(2325.84, preview.amount!!, 0.01)
        assertEquals(742.50, preview.miles!!, 0.01)
        assertTrue(preview.pointB!!.contains("ELKRIDGE", ignoreCase = true))
    }

    companion object {
        private const val REF_MILLIS = 1_753_401_600_000L // 2025-07-25

        private val IEL_RATE_CON = """
            Rate Confirmation IEL PO#: 2665704
            Integrity Express Logistics
            PO Box 42275 - Cincinnati, OH 45242
            Phone: (937) 329-9125 Ext: 9125
            Load Information
            IEL PO#: 2665704 Trailer: Reefer
            Team
            Size: 53 ft Temp: 38
            Pick Up: 07/25/25 Delivery: 07/26/25 Weight: 42000 CONTINUOUS RUN
            Miles: 742.50
            Carrier: UMT GLOBAL LOGISTICS LLC
            Estimated Rate (To Truck): $2,325.84 Unloading: $0.00 Total: $2,325.84
            Rate Description Quantity Total
            $2,325.84 Flat 1.00 $2,325.84
            Pick Ups
            Shed:FACTOR LAKE ZURICH Address: 1325 ENSELL RD LAKE ZURICH, IL 60047
            Phone: Date: 07/25/25 Time: 2PM
            RECOMMENDED NEARBY LOCATIONS: LAKE FOREST OASIS
            TRAVEL PLAZA / LOVES TRAVEL STOP #800 - 1900 BUSSE ROAD
            Deliveries
            Shed:VEHO-BALMD Address: 7091 TROY HILL DRIVE ELKRIDGE, MD 21075
            Date: 07/26/25 Time: 9PM
            xvi. A fee of $7.50 per pallet will be charged on loads that the carrier is responsible
            to supply pallets for exchange and they do not.
            xii. $35 will be deducted from your invoice for each comcheck issued for a fuel or cash advance.
        """.trimIndent()
    }
}
