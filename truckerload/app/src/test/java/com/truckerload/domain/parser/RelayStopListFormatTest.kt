package com.truckerload.domain.parser

import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Amazon Relay "📍1#:" stop-list format with Unicode-bold labels and emoji. */
class RelayStopListFormatTest {

    private val sample = """
🗺𝗧𝗿𝗶𝗽 𝗜𝗗 : T-114R1S5MH

📍1#: 115DZR7G2
Loaded - Preloaded
Fri, Nov 27, 10:45 AM EST
 CMH4
 1550 W Main St
 West Jefferson, OH 43162-9747
—————————————
📍2#: 115DZR7G2
Loaded - Preloaded
Fri, Nov 27, 07:51 PM EST
 WWS1
 776 Saegers Station Rd
 Montgomery, PA 17752
—————————————
📍3#: 11215K1PW
Empty - Drop
Fri, Nov 27, 10:09 PM EST
 QYY4
 251 W Woodhill
 Frailey Township, PA 17981-1639
—————————————
💰 𝗥𝗮𝘁𝗲: ${'$'}1771.47
 💰 Per mile: ${'$'}3.63/mi

🚛 𝗧𝗿𝗶𝗽: 487.37mi
 🕒 Duration: 0d 11h

❌Late PU: ${'$'}1000
❌Late DEL: ${'$'}700
❌No Update: ${'$'}200
❌No PU, DEL trailer photos: ${'$'}200
    """.trimIndent()

    private val ref = 1_795_000_000_000L // late Nov 2026

    private fun assertSample(text: String) {
        val loads = LoadMessageParser.parseAll(text, ref)
        assertEquals(1, loads.size)
        val load = loads[0]
        assertEquals("T-114R1S5MH", load.tripId)
        assertEquals(177_147L, load.totalRate.cents)
        assertEquals(487.37, load.totalMiles, 0.001)
        assertEquals(3, load.stops.size)
        assertEquals("West Jefferson, OH", load.pointA)
        assertEquals("Frailey Township, PA", load.pointB)
        assertEquals(listOf(StopType.PU, StopType.PU, StopType.DEL), load.stops.map { it.type })
        assertEquals("115DZR7G2", load.stops[0].puNumber)
        assertEquals("CMH4", load.stops[0].facilityCode)
        assertEquals("EST", load.stops[0].timezone)
        assertEquals("OH", load.stops[0].state)
        assertEquals("43162-9747", load.stops[0].zip)
        assertEquals("QYY4", load.stops[2].facilityCode)
        assertEquals("PA", load.stops[2].state)
        assertEquals("17981-1639", load.stops[2].zip)
        assertTrue(load.stops[0].fullAddress.contains("1550 W Main St"))
        assertTrue(load.stops[2].note.orEmpty().contains("Empty - Drop"))
        assertTrue(load.stops[0].scheduledTime.contains("10:45"))
        assertTrue(load.stops[2].scheduledTime.contains("10:09"))
    }

    @Test
    fun `styled relay stop list parses`() = assertSample(sample)

    @Test
    fun `plain ascii variant parses the same`() = assertSample(
        sample.replace("🗺", "").replace("📍", "").replace("💰", "").replace("🚛", "").replace("🕒", "")
            .replace("❌", "").replace("𝗧𝗿𝗶𝗽 𝗜𝗗", "Trip ID").replace("𝗥𝗮𝘁𝗲", "Rate").replace("𝗧𝗿𝗶𝗽", "Trip")
            .replace("—————————————", "-------------"),
    )

    @Test
    fun `penalty amounts are never the rate`() {
        val load = LoadMessageParser.parseOne(sample, ref)!!
        assertEquals(177_147L, load.totalRate.cents)
    }

    @Test
    fun `engine ranker picks the relay result`() {
        val loads = LoadParsingEngine.parse(ParseInput.PlainText(sample, ref)).getOrThrow()
        assertEquals(1, loads.size)
        assertEquals("T-114R1S5MH", loads[0].tripId)
        assertEquals(177_147L, loads[0].totalRate.cents)
        assertEquals(487.37, loads[0].totalMiles, 0.001)
    }

    @Test
    fun `missing rate still fills every other field as a draft`() {
        val noRate = sample.lines().filterNot { it.contains("𝗥𝗮𝘁𝗲") || it.contains("Per mile") }.joinToString("\n")
        val draft = MessageParseService().extractLoadFields(noRate, ref)
        assertEquals("T-114R1S5MH", draft.tripId)
        assertEquals("West Jefferson, OH", draft.pointA)
        assertEquals("Frailey Township, PA", draft.pointB)
        assertEquals(487.37, draft.parsedMiles(), 0.001)
        assertTrue("draft=$draft", LoadCompletenessChecker.of(draft).missingRequired.contains(LoadField.RATE))
    }

    @Test
    fun `middle stop follows its type hint`() {
        assertEquals(StopType.DEL, NumberedStopListParser.typeFromHint("Empty - Drop"))
        assertEquals(StopType.DEL, NumberedStopListParser.typeFromHint("Live Unload"))
        assertEquals(StopType.PU, NumberedStopListParser.typeFromHint("Loaded - Preloaded"))
        assertEquals(StopType.PU, NumberedStopListParser.typeFromHint("Live Load"))
    }

    @Test
    fun `Stop N variant with Rate and Distance labels and a live unload middle stop`() {
        val text = """
            Trip ID: T-ABC12345
            Stop 1: 1AAA1111
            Live Load
            Mon, Dec 1, 08:00 AM CST
            DFW7
            100 Main St
            Dallas, TX 75001
            ---
            Stop 2: 1BBB2222
            Live Unload
            Mon, Dec 1, 02:00 PM CST
            OKC2
            5 Oak Ave
            Oklahoma City, OK 73101
            ---
            Stop 3: 1CCC3333
            Empty - Drop
            Mon, Dec 1, 09:00 PM CST
            WIC3
            9 Elm Rd
            Wichita, KS 67201
            Payout: ${'$'}1,200.00
            Distance: 500 mi
        """.trimIndent()
        val load = LoadMessageParser.parseOne(text, ref)!!
        assertEquals(listOf(StopType.PU, StopType.DEL, StopType.DEL), load.stops.map { it.type })
        assertEquals("Dallas, TX", load.pointA)
        assertEquals("Wichita, KS", load.pointB)
        assertEquals(120_000L, load.totalRate.cents)
        assertEquals(500.0, load.totalMiles, 0.001)
    }
}
