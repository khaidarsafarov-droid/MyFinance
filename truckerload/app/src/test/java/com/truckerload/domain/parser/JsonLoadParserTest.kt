package com.truckerload.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonLoadParserTest {

    @Test
    fun parsesFlatBrokerObject() {
        val load = JsonLoadParser.parseOne(
            """
            {
              "trip_id": "T-88231",
              "total_rate": 2450.75,
              "total_miles": 910,
              "pickup": "Garner, NC",
              "delivery": "Dallas, TX",
              "pickup_date": "2026-08-20"
            }
            """.trimIndent(),
            referenceMillis = REF,
        )
        assertNotNull(load)
        assertEquals("T-88231", load!!.tripId)
        assertEquals(2450.75, load.totalRate, 0.01)
        assertEquals(910.0, load.totalMiles, 0.01)
        assertEquals("Garner, NC", load.pointA)
        assertEquals("Dallas, TX", load.pointB)
        assertEquals("2026-08-20", load.date)
    }

    @Test
    fun readsVendorAliasesAndMoneyStrings() {
        val load = JsonLoadParser.parseOne(
            """
            {"loadNumber":"RC-5521","lineHaul":"${'$'}1,800.00","mileage":"620",
             "origin":"Houston, TX","destination":"Atlanta, GA"}
            """.trimIndent(),
            referenceMillis = REF,
        )
        assertNotNull(load)
        assertEquals("RC-5521", load!!.tripId)
        assertEquals(1800.0, load.totalRate, 0.01)
        assertEquals(620.0, load.totalMiles, 0.01)
    }

    @Test
    fun flattensNestedAddressObjects() {
        val load = JsonLoadParser.parseOne(
            """
            {
              "rate": 1500,
              "origin": {"city": "Perrysburg", "state": "OH", "zip": "43551"},
              "destination": {"city": "Elkridge", "state": "MD"}
            }
            """.trimIndent(),
            referenceMillis = REF,
        )
        assertNotNull(load)
        assertTrue(load!!.pointA.contains("Perrysburg"))
        assertTrue(load.pointB.contains("Elkridge"))
    }

    @Test
    fun parsesArrayOfLoadsAndCollectionWrapper() {
        val fromArray = JsonLoadParser.parseAll(
            """
            [{"rate":1000,"from":"Reno, NV","to":"Boise, ID"},
             {"rate":2000,"from":"Boise, ID","to":"Ogden, UT"}]
            """.trimIndent(),
            referenceMillis = REF,
        )
        assertEquals(2, fromArray.size)

        val wrapped = JsonLoadParser.parseAll(
            """{"loads":[{"rate":1200,"origin":"Reno, NV","destination":"Boise, ID"}]}""",
            referenceMillis = REF,
        )
        assertEquals(1, wrapped.size)
        assertEquals(1200.0, wrapped.first().totalRate, 0.01)
    }

    @Test
    fun usesStopsArrayWhenNoLabeledAddresses() {
        val load = JsonLoadParser.parseOne(
            """
            {"amount": 3100, "stops": [
               {"city": "Laredo", "state": "TX"},
               {"city": "Memphis", "state": "TN"}
            ]}
            """.trimIndent(),
            referenceMillis = REF,
        )
        assertNotNull(load)
        assertTrue(load!!.pointA.contains("Laredo"))
        assertTrue(load.pointB.contains("Memphis"))
    }

    @Test
    fun generatesTripIdWhenPayloadHasNone() {
        val load = JsonLoadParser.parseOne(
            """{"rate":900,"pickup":"Reno, NV","delivery":"Boise, ID"}""",
            referenceMillis = REF,
        )
        assertNotNull(load)
        assertTrue(load!!.tripId.isNotBlank())
    }

    @Test
    fun rejectsPayloadsWithoutRateOrRoute() {
        assertNull(JsonLoadParser.parseOne("""{"pickup":"Reno, NV"}"""))
        assertNull(JsonLoadParser.parseOne("""{"rate":0,"pickup":"Reno, NV"}"""))
        assertNull(JsonLoadParser.parseOne("""{"rate":1200}"""))
        assertNull(JsonLoadParser.parseOne("not json at all"))
    }

    @Test
    fun readsLoadsOutOfTelegramJsonExport() {
        val export = """
            {"name":"Relay","type":"personal_chat","id":1,"messages":[
              {"id":1,"type":"message","date_unixtime":"1753401600",
               "text":"Trip ID: T-116KYL6KW\nTotal Rate: 2500.00\nTotal Loaded Miles: 850 mi\nPu-address: SWF2, Garner, NC"}
            ]}
        """.trimIndent()
        val loads = JsonLoadParser.parseAll(export, referenceMillis = REF)
        assertEquals(1, loads.size)
        assertEquals(2500.0, loads.first().totalRate, 0.01)
    }

    @Test
    fun flattenToTextExposesUnknownKeys() {
        val text = JsonLoadParser.flattenToText(
            """{"weird_rate_field":"2500","detail":{"weird_origin":"Garner, NC"}}""",
        )
        assertTrue(text.contains("weird_rate_field: 2500"))
        assertTrue(text.contains("weird_origin: Garner, NC"))
    }

    @Test
    fun messageParseServiceAcceptsJsonPayload() {
        val loads = MessageParseService().parseLoadsFromInboundText(
            """{"rate":2450.75,"miles":910,"pickup":"Garner, NC","delivery":"Dallas, TX"}""",
            REF,
            "load.json",
        ).getOrThrow()
        assertEquals(1, loads.size)
        assertEquals(2450.75, loads.first().totalRate, 0.01)
    }

    @Test
    fun unknownJsonKeysStillResolveViaFlattenedFallback() {
        val load = MessageParseService().parseLoadFromUserInput(
            """{"cargo":{"Total Rate":"2500.00","Pu-address":"SWF2, Garner, NC"}}""",
            REF,
        ).getOrThrow()
        assertEquals(2500.0, load.totalRate, 0.01)
        assertTrue(load.pointA.contains("Garner"))
    }

    private companion object {
        const val REF = 1_776_441_600_000L
    }
}
