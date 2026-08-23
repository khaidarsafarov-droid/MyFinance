package com.truckerload.domain.import.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramJsonExportParserTest {

    private val parser = TelegramJsonExportParser()

    @Test
    fun isTelegramJsonExport_detectsResultJsonShape() {
        val json = """
            {
              "name": "Test chat",
              "type": "private_supergroup",
              "id": 1,
              "messages": []
            }
        """.trimIndent()
        assertTrue(TelegramJsonExportParser.isTelegramJsonExport(json))
    }

    @Test
    fun parse_extractsLoadFromMessage() {
        val json = """
            {
              "name": "Relay",
              "type": "private_supergroup",
              "id": 1,
              "messages": [
                {
                  "id": 10,
                  "type": "message",
                  "date": "2025-06-28T14:30:00",
                  "date_unixtime": "1751113800",
                  "from": "bruce",
                  "text": "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116KYL6KW\n\nPU# 115S1Q2P1 \n\nNote: Empty trailer\n\nPu-time: 06/28 03:00 EDT\nPu-address: SWF2\n76 Patriot Way\nHopewell Junction, NY 12533\n\nDel-time: 06/29 07:00 EDT\nDel-address: TOL3\n9240 Fremont Pike\nPERRYSBURG, Ohio 43551\n\nTotal Rate: ${'$'}2945.56\nTotal Loaded Miles: 1198.03 mi\n"
                }
              ]
            }
        """.trimIndent()
        val loads = parser.parse(json)
        assertEquals(1, loads.size)
        assertEquals("T-116KYL6KW", loads[0].tripId)
        assertEquals("2025-06-28", loads[0].date)
        assertTrue(loads[0].parsedAt > 0L)
    }

    @Test
    fun parse_august2025Message_keepsLastYearNotThisWeek() {
        // Regression: Pu-time 08/21 from a 2025-08-20 Telegram message must not become 2026-08-21.
        val json = """
            {
              "name": "#8860 Khaidar Safarov SOLO",
              "type": "private_supergroup",
              "id": 2868419661,
              "messages": [
                {
                  "id": 709,
                  "type": "message",
                  "date": "2025-08-20T14:16:54",
                  "date_unixtime": "1755717414",
                  "from": "bruce",
                  "text": "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-112QX54Y8\n  \nPU# 112Y7C36C \n\nNote: Empty trailer\n\nPu-time: 08/21 01:39 EDT\nPu-address: MDT5\n200 Goodman Dr\nLEWISBERRY, PA 17339\n\nDel-time: 08/21 03:32 EDT\nDel-address: VENDOR-165096271\n5197 COMMERCE DR\nYORK, PA 17408\n\nTotal Rate: ${'$'}1197.76\nTotal Loaded Miles: 424.86 mi\n"
                },
                {
                  "id": 872,
                  "type": "message",
                  "date": "2025-08-22T10:05:22",
                  "date_unixtime": "1755875122",
                  "from": "bruce",
                  "text": "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116C43HGC\n  \nPU# 112HH4TDW \n\nNote: Empty trailer\n\nPu-time: 08/22 22:44 EDT\nPu-address: ABE4, EASTON, PA\n\nDel-time: 08/23 01:05 EDT\nDel-address: VENDOR, Mount Olive, NJ\n\nTotal Rate: ${'$'}3768.15\nTotal Loaded Miles: 2205.31 mi\n"
                },
                {
                  "id": 910,
                  "type": "message",
                  "date": "2025-08-22T13:36:59",
                  "date_unixtime": "1755887819",
                  "from": "bruce",
                  "text": "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-112VZ3TL2\n  \nPU# 114BHRKRQ \n\nNote: Preloaded\n\nPu-time: 08/22 23:28 EDT\nPu-address: ABE8, FLORENCE, NJ\n\nDel-time: 08/24 00:15 EDT\nDel-address: TYS1, Rockford, TN\n\nTotal Rate: ${'$'}2562.72\nTotal Loaded Miles: 1321.75 mi\n"
                }
              ]
            }
        """.trimIndent()
        val loads = parser.parse(json).associateBy { it.tripId }
        assertEquals(3, loads.size)
        assertEquals("2025-08-21", loads.getValue("T-112QX54Y8").date)
        assertEquals(2025, loads.getValue("T-112QX54Y8").year)
        assertEquals("2025-08-22", loads.getValue("T-116C43HGC").date)
        assertEquals(2025, loads.getValue("T-116C43HGC").year)
        assertEquals("2025-08-22", loads.getValue("T-112VZ3TL2").date)
        assertEquals(2025, loads.getValue("T-112VZ3TL2").year)
    }

    @Test
    fun parse_emptyMessages_zeroCount() {
        val json = """
            {
              "name": "Relay",
              "type": "private_supergroup",
              "id": 1,
              "messages": []
            }
        """.trimIndent()
        val loads = parser.parse(json)
        assertEquals(0, loads.size)
        val result = com.truckerload.utils.LoadImporter.ImportResult(
            imported = 0,
            skipped = 0,
            parsed = loads.size,
        )
        assertEquals(0, result.parsed)
    }

    @Test
    fun parse_twoLoads_countMatches() {
        val json = """
            {
              "name": "Relay",
              "type": "private_supergroup",
              "id": 1,
              "messages": [
                {
                  "id": 1,
                  "type": "message",
                  "from": "bot",
                  "text": "Trip ID: T-AAA\nTotal Rate: ${'$'}1000.00\nTotal Loaded Miles: 400 mi\nPu-address: SWF2, Hopewell Junction, NY\nDel-address: TOL3, Perrysburg, OH"
                },
                {
                  "id": 2,
                  "type": "message",
                  "from": "bot",
                  "text": "Trip ID: T-BBB\nTotal Rate: ${'$'}2000.00\nTotal Loaded Miles: 800 mi\nPu-address: SWF2, Hopewell Junction, NY\nDel-address: TOL3, Perrysburg, OH"
                }
              ]
            }
        """.trimIndent()
        val loads = parser.parse(json)
        assertEquals(2, loads.size)
        val result = com.truckerload.utils.LoadImporter.ImportResult(
            imported = loads.size,
            skipped = 0,
            parsed = loads.size,
        )
        assertEquals(2, result.parsed)
        assertEquals(2, result.imported)
    }
}
