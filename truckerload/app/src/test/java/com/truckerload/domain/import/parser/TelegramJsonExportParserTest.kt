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
                  "from": "bruce",
                  "text": "𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116KYL6KW\n\nPU# 115S1Q2P1 \n\nNote: Empty trailer\n\nPu-time: 06/28 03:00 EDT\nPu-address: SWF2\n76 Patriot Way\nHopewell Junction, NY 12533\n\nDel-time: 06/29 07:00 EDT\nDel-address: TOL3\n9240 Fremont Pike\nPERRYSBURG, Ohio 43551\n\nTotal Rate: ${'$'}2945.56\nTotal Loaded Miles: 1198.03 mi\n"
                }
              ]
            }
        """.trimIndent()
        val loads = parser.parse(json)
        assertEquals(1, loads.size)
        assertEquals("T-116KYL6KW", loads[0].tripId)
    }
}
