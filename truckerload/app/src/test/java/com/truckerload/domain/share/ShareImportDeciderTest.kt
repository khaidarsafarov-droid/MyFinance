package com.truckerload.domain.share

import com.truckerload.domain.money.Money
import com.truckerload.domain.parser.LoadField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareImportDeciderTest {

    private val now = 1_776_441_600_000L

    @Test
    fun relayText_parsesIntoALoad() {
        val decision = decider().decide(RELAY, "relay.txt", "hash-relay", MemoryShareImportIndex())
        val add = decision as ShareImportDecision.Add
        assertEquals("T-116KYL6KW", add.load.tripId)
        assertEquals(Money.fromDollars(2500.0), add.load.totalRate)
        assertEquals(850.0, add.load.totalMiles, 0.01)
        assertTrue(add.load.pointA.contains("Garner"))
        assertTrue(add.load.pointB.contains("Dallas"))
        assertTrue(LoadField.RATE !in add.missing)
        assertTrue(LoadField.PICKUP !in add.missing)
        assertTrue(LoadField.DELIVERY !in add.missing)
    }

    @Test
    fun brokerPdfText_parsesBrokerOriginAndRate() {
        val decision = decider().decide(
            BROKER,
            "carrier-rate-confirmation.pdf",
            "hash-broker",
            MemoryShareImportIndex(),
        )
        val add = decision as ShareImportDecision.Add
        assertTrue(add.load.tripId.contains("99881"))
        assertEquals(Money.fromDollars(1800.0), add.load.totalRate)
        assertEquals(620.0, add.load.totalMiles, 0.01)
        assertTrue(add.load.pointA.contains("Houston"))
        assertTrue(add.load.pointB.contains("Atlanta"))
        assertEquals("Prairie Lane Logistics", add.broker)
        assertEquals("2026-08-18", add.load.date)
        assertTrue(add.missing.isEmpty())
    }

    @Test
    fun dispatchAndTender_parseAsLoads() {
        val dispatch = decider().decide(DISPATCH, null, "hash-d", MemoryShareImportIndex())
        val tender = decider().decide(TENDER, null, "hash-t", MemoryShareImportIndex())
        assertTrue(dispatch is ShareImportDecision.Add)
        assertTrue(tender is ShareImportDecision.Add)
        assertEquals(Money.fromDollars(3200.0), (tender as ShareImportDecision.Add).load.totalRate)
        assertTrue(tender.load.pointA.contains("Dallas"))
        assertTrue(tender.load.pointB.contains("Phoenix"))
    }

    @Test
    fun partialHighConfidence_savesAndMarksHoles() {
        val decision = decider().decide(PARTIAL, "ratecon.pdf", "hash-p", MemoryShareImportIndex())
        val add = decision as ShareImportDecision.Add
        assertTrue(add.load.tripId.contains("88421"))
        assertTrue(add.load.pointA.contains("Dallas"))
        assertTrue(LoadField.RATE in add.missing)
        assertTrue(LoadField.DELIVERY in add.missing || add.load.pointB.isNotBlank())
        assertTrue(LoadField.MILES in add.missing)
    }

    @Test
    fun chatReceiptAndPhoto_addNothing() {
        val index = MemoryShareImportIndex()
        val decider = decider()
        assertTrue(decider.decide(CHAT, null, "h1", index) is ShareImportDecision.Reject)
        assertTrue(decider.decide(RECEIPT, "receipt.jpg", "h2", index) is ShareImportDecision.Reject)
        assertTrue(
            decider.decide("blue sky and a parking lot", "IMG_100.jpg", "h3", index)
                is ShareImportDecision.Reject,
        )
    }

    @Test
    fun sameLoadNumberAndBroker_isDuplicate() {
        val index = MemoryShareImportIndex()
        val first = decider().decide(BROKER, "a.pdf", "hash-a", index) as ShareImportDecision.Add
        index.remember(first.load.id, first.loadNumber, first.broker, first.fileHash)
        val pasted = BROKER.replace("Miles: 620", "Miles: 640")
        val second = decider().decide(pasted, "paste.txt", "hash-b", index)
        assertEquals(first.load.id, (second as ShareImportDecision.Duplicate).existingLoadId)
    }

    @Test
    fun sameFileHash_isDuplicate() {
        val index = MemoryShareImportIndex()
        val first = decider().decide(RELAY, null, "same-hash", index) as ShareImportDecision.Add
        index.remember(first.load.id, first.loadNumber, first.broker, first.fileHash)
        val second = decider().decide(RELAY, null, "same-hash", index)
        assertTrue(second is ShareImportDecision.Duplicate)
    }

    @Test
    fun existingTripId_isDuplicate() {
        val decision = decider().decide(RELAY, null, "new-hash", MemoryShareImportIndex()) { trip ->
            if (trip.equals("T-116KYL6KW", ignoreCase = true)) "already" else null
        }
        assertEquals("already", (decision as ShareImportDecision.Duplicate).existingLoadId)
    }

    private fun decider() = ShareImportDecider(nowMillis = now)

    private companion object {
        val RELAY = """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
            Del-address: DAL3, Dallas, TX
        """.trimIndent()

        val BROKER = """
            RATE CONFIRMATION
            Load #: RC-99881
            Broker: Prairie Lane Logistics
            MC# 778812
            Carrier: Demo Freight LLC
            Line Haul: $1800
            Miles: 620
            Origin: Houston, TX 77001
            Destination: Atlanta, GA 30301
            Date: 08/18/2026
        """.trimIndent()

        val DISPATCH = """
            DISPATCH SHEET
            Load # DSP-4401
            Broker: Harbor Freight Brokers
            MC# 441200
            Carrier: Sample Carrier
            Shipper: North Mill, Omaha, NE 68102
            Pickup date: 04/11/2026
            Consignee: Red Dock, Kansas City, MO 64101
            Delivery: Kansas City, MO 64101
            Rate: $1,875.00
            Miles: 190
        """.trimIndent()

        val TENDER = """
            Load Tender
            PO # 550192
            Broker: Canyon Bridge
            MC# 309441
            Carrier: Demo Hauling LLC
            Origin: Dallas, TX 75201
            Destination: Phoenix, AZ 85001
            Pickup date: 06/02/2026
            Delivery date: 06/04/2026
            Line Haul: $3,200.00
            Miles: 1065
        """.trimIndent()

        val PARTIAL = """
            RATE CONFIRMATION
            Load # 88421
            Broker: Northline Logistics
            MC# 552190
            Carrier: Demo Hauling
            Pickup: Dallas, TX 75201
            Date: 03/12/2026
            Consignee: River Mill
        """.trimIndent()

        const val CHAT = "Hey, are you free tonight? Let's grab dinner near the hotel."

        val RECEIPT = """
            WALMART SUPERCENTER
            Thank you for shopping
            Subtotal $38.12
            Tax $2.40
            Total $40.52
            VISA CREDIT
            """.trimIndent()
    }
}
