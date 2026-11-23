package com.truckerload.domain.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareLoadClassifierTest {

    @Test
    fun relayTrip_isALoad() {
        val reading = ShareLoadClassifier.read(RELAY)
        assertTrue(reading.accept)
        assertEquals("T-116KYL6KW", reading.loadNumber)
        assertEquals(2500.0, reading.rate!!, 0.01)
        assertTrue(reading.pickup.contains("Garner"))
        assertTrue(reading.delivery.contains("Dallas"))
        assertEquals(850.0, reading.miles!!, 0.01)
    }

    @Test
    fun brokerRateConfirmation_isALoad() {
        val reading = ShareLoadClassifier.read(BROKER, "carrier-rate-confirmation.pdf")
        assertTrue(reading.accept)
        assertEquals("RC-99881", reading.loadNumber)
        assertEquals("Prairie Lane Logistics", reading.broker)
        assertEquals(1800.0, reading.rate!!, 0.01)
        assertTrue(reading.pickup.contains("Houston"))
        assertTrue(reading.delivery.contains("Atlanta"))
        assertEquals("08/18/2026", reading.dateRaw)
    }

    @Test
    fun dispatchSheet_isALoad() {
        val reading = ShareLoadClassifier.read(DISPATCH)
        assertTrue(reading.accept)
        assertEquals("DSP-4401", reading.loadNumber)
        assertTrue(reading.pickup.contains("Omaha"))
        assertTrue(reading.delivery.contains("Kansas City"))
        assertEquals(1875.0, reading.rate!!, 0.01)
    }

    @Test
    fun russianRateConfirmation_isALoad() {
        val reading = ShareLoadClassifier.read(RUSSIAN)
        assertTrue(reading.accept)
        assertEquals("RU-2044", reading.loadNumber)
        assertEquals("Северная Линия", reading.broker)
        assertTrue(reading.pickup.contains("Dallas"))
        assertTrue(reading.delivery.contains("Atlanta"))
        assertEquals(2100.0, reading.rate!!, 0.01)
    }

    @Test
    fun partialRateConfirmation_stillHighConfidence() {
        val reading = ShareLoadClassifier.read(PARTIAL)
        assertTrue(reading.accept)
        assertEquals("88421", reading.loadNumber)
        assertFalse(reading.hasRate)
        assertTrue(reading.pickup.contains("Dallas"))
        assertTrue(reading.delivery.isBlank())
    }

    @Test
    fun chatMessage_isNotALoad() {
        val reading = ShareLoadClassifier.read(
            "Hey, are you free tonight? Let's grab dinner near the hotel.",
        )
        assertFalse(reading.accept)
    }

    @Test
    fun storeReceipt_isNotALoad() {
        val reading = ShareLoadClassifier.read(
            """
            WALMART SUPERCENTER
            Thank you for shopping
            Subtotal $38.12
            Tax $2.40
            Total $40.52
            VISA CREDIT
            """.trimIndent(),
        )
        assertFalse(reading.accept)
    }

    @Test
    fun fuelReceipt_isNotALoad() {
        val reading = ShareLoadClassifier.read(
            """
            Pilot #442
            Diesel receipt
            42.510 gallons
            Pump total $186.40
            """.trimIndent(),
        )
        assertFalse(reading.accept)
        assertTrue(reading.negative >= 4)
    }

    @Test
    fun photoCaption_isNotALoad() {
        val reading = ShareLoadClassifier.read(
            "Parking lot sunset. No paperwork in this photo. IMG_2048",
            fileName = "IMG_2048.jpg",
        )
        assertFalse(reading.accept)
    }

    @Test
    fun emptyImageText_isNotALoad() {
        assertFalse(ShareLoadClassifier.read("", fileName = "scan.jpg").accept)
    }

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
            Broker: Prairie Lane Logistics
            MC# 778812
            Carrier: Demo Freight LLC
            Shipper: North Mill, Omaha, NE 68102
            Pickup date: 04/11/2026
            Consignee: Red Dock
            Delivery: Kansas City, MO 64101
            Rate: $1,875.00
            Miles: 190
        """.trimIndent()

        val RUSSIAN = """
            ПОДТВЕРЖДЕНИЕ СТАВКИ
            Номер груза: RU-2044
            Брокер: Северная Линия
            Перевозчик: Demo Hauling
            Погрузка: Dallas, TX 75201
            Разгрузка: Atlanta, GA 30301
            Дата: 03/12/2026
            Ставка: $2,100.00
            Мили: 780
            MC# 100200
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
    }
}
