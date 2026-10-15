package com.truckerload.domain.parser

import com.truckerload.domain.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerRateTemplateEngineTest {

    @Test
    fun catalogLoadsTheBrokerSet() {
        assertEquals(10, BrokerRateTemplateCatalog.size())
    }

    @Test
    fun readsBrokerLoadNumberAndRate() {
        assertLoad("tql", TQL, "8844221", 1850.00)
        assertLoad("ch", CH_ROBINSON, "55CH9911", 3200.00)
        assertLoad("echo", ECHO, "EC778899", 1111.00)
        assertLoad("jb", JB_HUNT, "7JH22001", 2450.50)
        assertLoad("uber", UBER, "UF300400", 2222.00)
        assertLoad("schneider", SCHNEIDER, "SHP100200", 1775.25)
        assertLoad("landstar", LANDSTAR, "FB445566", 990.00)
        assertLoad("coyote", COYOTE, null, 2010.00)
        assertLoad("arrive", ARRIVE, null, 1640.00)
    }

    @Test
    fun integrityExpressTemplateMatchesTheExistingParser() {
        val load = BrokerRateTemplateEngine.parseOne(IEL, nowMillis = REF_MILLIS)
        assertNotNull(load)
        assertEquals(Money.fromDollars(2325.84), load!!.totalRate)
        assertEquals(742.50, load.totalMiles, 0.01)
        assertTrue(load.tripId.contains("2665704"))
        assertTrue(load.pointA.contains("LAKE ZURICH", ignoreCase = true))
        assertTrue(load.pointB.contains("ELKRIDGE", ignoreCase = true))
    }

    @Test
    fun unknownBrokerFallsThrough() {
        assertNull(
            BrokerRateTemplateEngine.parseOne(
                """
                Fuel Receipt
                Pilot
                Diesel
                Total Amount: $180.00
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun linehaulFuelAndAccessorialsMustEqualTheTotalInCents() {
        assertEquals(true, BrokerRateTemplateEngine.partsAgreeWithTotal(SCHNEIDER))
        val short = SCHNEIDER.replace("Total $1,775.25", "Total $1,700.00")
        assertEquals(false, BrokerRateTemplateEngine.partsAgreeWithTotal(short))
        assertNotNull(BrokerRateTemplateEngine.parseOne(short, nowMillis = REF_MILLIS))
    }

    @Test
    fun inboundTextKeepsTheBrokerRate() {
        val loads = MessageParseService()
            .parseLoadsFromInboundText(TQL, REF_MILLIS, "tql-rate-confirmation.pdf")
            .getOrThrow()
        assertEquals(1, loads.size)
        assertEquals(Money.fromDollars(1850.00), loads.first().totalRate)
        assertTrue(loads.first().tripId.contains("8844221"))
    }

    private fun assertLoad(name: String, text: String, trip: String?, rate: Double) {
        val load = BrokerRateTemplateEngine.parseOne(text, nowMillis = REF_MILLIS)
        assertNotNull(name, load)
        assertEquals(name, Money.fromDollars(rate), load!!.totalRate)
        if (trip != null) assertTrue("$name trip ${load.tripId}", load.tripId.contains(trip))
        assertFalse(load.pointA.isBlank())
        assertFalse(load.pointB.isBlank())
    }

    companion object {
        private const val REF_MILLIS = 1_753_401_600_000L

        private const val STOPS = """
            Pick Ups
            Address: 100 Main St Dallas, TX 75001
            Deliveries
            Address: 200 Oak Ave Denver, CO 80202
            Miles: 900
        """

        private val TQL = """
            TQL
            Load Information
            PO# 8844221
            Total: $1,850.00
            $STOPS
        """.trimIndent()

        private val CH_ROBINSON = """
            C.H. Robinson
            confirmation - # 55CH9911
            total $3,200.00
            $STOPS
        """.trimIndent()

        private val ECHO = """
            Echo
            service for load # EC778899
            pay summary
            total $1,111.00
            $STOPS
        """.trimIndent()

        private val JB_HUNT = """
            J.B. Hunt
            Carrier Confirmation for Load 7JH22001
            Total Rate: $2,450.50
            $STOPS
        """.trimIndent()

        private val UBER = """
            Uber Freight
            Load # UF300400
            Total Rate $2,222.00
            $STOPS
        """.trimIndent()

        private val SCHNEIDER = """
            Schneider
            Shipment ID: SHP100200
            Linehaul $1,500.00
            Fuel Surcharge $200.00
            Accessorials $75.25
            AGREED TO RATE
            Total $1,775.25
            $STOPS
        """.trimIndent()

        private val LANDSTAR = """
            Landstar
            fb #: FB445566
            carrier pay: $990.00
            $STOPS
        """.trimIndent()

        private val COYOTE = """
            Coyote Logistics
            Total $2,010.00
            $STOPS
        """.trimIndent()

        private val ARRIVE = """
            Arrive Logistics
            Total $1,640.00
            $STOPS
        """.trimIndent()

        private val IEL = """
            Rate Confirmation IEL PO#: 2665704
            Integrity Express Logistics
            Load Information
            IEL PO#: 2665704
            Pick Up: 07/25/25
            Miles: 742.50
            Estimated Rate (To Truck): $2,325.84 Unloading: $0.00 Total: $2,325.84
            Pick Ups
            Address: 1325 ENSELL RD LAKE ZURICH, IL 60047
            Deliveries
            Address: 7091 TROY HILL DRIVE ELKRIDGE, MD 21075
        """.trimIndent()
    }
}
