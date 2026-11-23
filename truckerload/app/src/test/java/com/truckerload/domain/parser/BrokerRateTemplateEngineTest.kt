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
        assertEquals(Money.fromDollars(1864.20), load!!.totalRate)
        assertEquals(512.25, load.totalMiles, 0.01)
        assertTrue(load.tripId.contains("4401901"))
        assertTrue(load.pointA.contains("CEDAR RAPIDS", ignoreCase = true))
        assertTrue(load.pointB.contains("BOISE", ignoreCase = true))
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
        val check = BrokerRateTemplateEngine.ratePartCheck(short)
        assertEquals(170_000L, check?.labeledCents)
        assertEquals(177_525L, check?.partsCents)
        assertNotNull(BrokerRateTemplateEngine.parseOne(short, nowMillis = REF_MILLIS))
    }

    @Test
    fun amazonRelayWinsWhenABrokerTemplateAlsoMatches() {
        val mixed = """
            TQL
            Load Information
            PO# 8844221
            Total: $1,850.00
            Pick Ups
            Address: 100 Main St Dallas, TX 75001
            Deliveries
            Address: 200 Oak Ave Denver, CO 80202

            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-address: SWF2, Garner, NC
            Del-address: TOL3, Perrysburg, OH
        """.trimIndent()
        val loads = MessageParseService()
            .parseLoadsFromInboundText(mixed, REF_MILLIS)
            .getOrThrow()
        assertEquals(1, loads.size)
        assertEquals("T-116KYL6KW", loads.first().tripId)
        assertEquals(Money.fromDollars(2500.00), loads.first().totalRate)
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
            Rate Confirmation IEL PO#: 4401901
            Integrity Express Logistics
            Load Information
            IEL PO#: 4401901
            Pick Up: 07/25/25
            Miles: 512.25
            Carrier: Sample Haul LLC
            Estimated Rate (To Truck): $1,864.20 Unloading: $0.00 Total: $1,864.20
            Pick Ups
            Address: 400 DEMO DOCK RD CEDAR RAPIDS, IA 52404
            Deliveries
            Address: 900 SAMPLE LN BOISE, ID 83702
        """.trimIndent()
    }
}
