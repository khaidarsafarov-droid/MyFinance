package com.truckerload.domain.parser

import com.truckerload.domain.ingest.InboundDocumentResolver
import com.truckerload.domain.ingest.ReceiptFieldExtractor
import com.truckerload.domain.ingest.ReceiptKind
import com.truckerload.domain.ingest.ReceiptKindClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.money.Money

class RateConfirmationLoadParserTest {

    @Test
    fun parsesIntegrityExpressRateConfirmation() {
        val load = RateConfirmationLoadParser.parseOne(
            FakeRateConfirmation.TEXT,
            nowMillis = REF_MILLIS,
            fileName = FakeRateConfirmation.FILE_NAME,
        )
        assertNotNull(load)
        assertEquals(Money.fromDollars(FakeRateConfirmation.RATE), load!!.totalRate)
        assertEquals(FakeRateConfirmation.MILES, load.totalMiles, 0.01)
        assertTrue(load.tripId.contains(FakeRateConfirmation.TRIP))
        assertTrue(load.pointA.contains(FakeRateConfirmation.PICKUP_CITY, ignoreCase = true))
        assertTrue(load.pointB.contains(FakeRateConfirmation.DELIVERY_CITY, ignoreCase = true))
        assertEquals("2025-07-25", load.date)
    }

    @Test
    fun ignoresPalletFeeAndLovesTruckStop() {
        val rate = RateConfirmationLoadParser.bestRate(FakeRateConfirmation.TEXT)
        assertEquals(FakeRateConfirmation.RATE, rate!!, 0.01)
        val (pickup, delivery) = RateConfirmationLoadParser.extractStops(FakeRateConfirmation.TEXT)
        assertFalse(pickup.contains("LOVES", ignoreCase = true))
        assertFalse(delivery.contains("LOVES", ignoreCase = true))
        assertFalse(pickup.contains(FakeRateConfirmation.HEADER_CITY, ignoreCase = true))
    }

    @Test
    fun inboundResolverAutoSavesLoadInsteadOfDieselPrompt() {
        val decision = InboundDocumentResolver.resolve(
            text = FakeRateConfirmation.TEXT,
            fileName = FakeRateConfirmation.FILE_NAME,
            referenceMillis = REF_MILLIS,
        )
        assertEquals(ReceiptKind.LOAD, decision.preview.kind)
        assertTrue(decision.autoSaveLoads)
        assertEquals(1, decision.loads.size)
        assertEquals(Money.fromDollars(FakeRateConfirmation.RATE), decision.loads.first().totalRate)
        assertEquals(FakeRateConfirmation.RATE, decision.preview.amount!!, 0.01)
        assertTrue(decision.preview.pointA!!.contains(FakeRateConfirmation.PICKUP_CITY, ignoreCase = true))
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
            .parseLoadsFromInboundText(FakeRateConfirmation.TEXT, REF_MILLIS, "rate-confirmation.pdf")
            .getOrThrow()
        assertEquals(1, loads.size)
        assertEquals(Money.fromDollars(FakeRateConfirmation.RATE), loads.first().totalRate)
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
    fun receiptPreviewDoesNotHighlightPalletFee() {
        val preview = ReceiptFieldExtractor.extract(
            FakeRateConfirmation.TEXT,
            fileName = FakeRateConfirmation.FILE_NAME,
        )
        assertEquals(ReceiptKind.LOAD, preview.kind)
        assertEquals(FakeRateConfirmation.RATE, preview.amount!!, 0.01)
        assertEquals(FakeRateConfirmation.MILES, preview.miles!!, 0.01)
        assertTrue(preview.pointB!!.contains(FakeRateConfirmation.DELIVERY_CITY, ignoreCase = true))
    }

    companion object {
        private const val REF_MILLIS = 1_753_401_600_000L // 2025-07-25
    }
}
