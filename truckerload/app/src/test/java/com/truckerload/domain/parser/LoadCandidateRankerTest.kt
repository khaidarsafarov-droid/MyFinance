package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.money.Money

class LoadCandidateRankerTest {

    @Test
    fun prefersEstimatedRateWhenFirstTotalRateIsAFee() {
        val text = """
            Rate Confirmation
            Trip ID: T-88001
            Total Rate: 50.00
            Estimated Rate (To Truck): ${'$'}2,100.00
            Miles: 700
            Pick Ups
            Address: 100 Main St Garner, NC 27529
            Deliveries
            Address: 200 Oak Ave Dallas, TX 75201
        """.trimIndent()

        val load = MessageParseService().parseLoadFromUserInput(text).getOrThrow()
        assertEquals(Money.fromDollars(2100.0), load.totalRate)
        assertEquals(700.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Garner"))
        assertTrue(load.pointB.contains("Dallas"))
    }

    @Test
    fun spreadsheetHeadersGrossShipperConsignee() {
        val text = """
            Load Number,Gross,Distance,Shipper,Consignee
            T-88002,1800.00,600,"Garner, NC","Dallas, TX"
        """.trimIndent()

        val load = MessageParseService().parseLoadsFromInboundText(text).getOrThrow().firstOrNull()
        assertEquals("T-88002", load!!.tripId)
        assertEquals(Money.fromDollars(1800.0), load.totalRate)
        assertEquals(600.0, load.totalMiles, 0.01)
        assertTrue(load.pointA.contains("Garner"))
        assertTrue(load.pointB.contains("Dallas"))
    }

    @Test
    fun paycheckStatementIsNotSavedAsALoad() {
        val loads = MessageParseService().parseLoadsFromInboundText(
            """
            Driver Settlement
            Payee ID: 268709
            Net Settlement: ${'$'}4,040.30
            Grand Total ${'$'}7,440.03
            """.trimIndent(),
        ).getOrThrow()
        assertTrue(loads.isEmpty())
    }

    @Test
    fun dieselGradeIsNotGallonsWhenItDisagreesWithTheTotal() {
        val parsed = DieselTextParser.parse(
            """
            PILOT TRAVEL CENTER
            DIESEL #2
            2 GAL
            PRICE/GAL ${'$'}3.899
            PUMP TOTAL ${'$'}469.99
            """.trimIndent(),
        )
        assertEquals(469.99, parsed!!.totalAmount, 0.01)
        assertEquals(120.54, parsed.gallons!!, 0.05)
        assertEquals(3.899, parsed.pricePerGallon!!, 0.001)
    }

    @Test
    fun twoRealGallonsStayWhenTheTotalAgrees() {
        val fields = DieselReceiptExtractor.extract(
            """
            DIESEL #2
            2 GAL
            PRICE/GAL ${'$'}3.90
            PUMP TOTAL ${'$'}7.80
            """.trimIndent(),
        )
        assertEquals(2.0, fields.gallons!!, 0.01)
    }
}
