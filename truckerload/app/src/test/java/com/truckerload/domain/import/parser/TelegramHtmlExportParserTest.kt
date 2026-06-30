package com.truckerload.domain.import.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramHtmlExportParserTest {

    private val parser = TelegramHtmlExportParser()

    @Test
    fun isTelegramExport_detectsDesktopExportStructure() {
        val html = sampleTelegramExport(listOf(loadBlock("T-116KYL6KW")))
        assertTrue(TelegramHtmlExportParser.isTelegramExport(html))
    }

    @Test
    fun isTelegramExport_rejectsGenericHtml() {
        assertFalse(TelegramHtmlExportParser.isTelegramExport("<html><body><p>Trip ID: X</p></body></html>"))
    }

    @Test
    fun parse_extractsLoadsFromMessageDivs() {
        val html = sampleTelegramExport(
            listOf(
                loadBlock("T-116KYL6KW"),
                loadBlock("T-118KJ7LQW", rate = "1,800.00", miles = "850.50"),
            ),
        )
        val loads = parser.parse(html)
        assertEquals(2, loads.size)
        assertEquals("T-116KYL6KW", loads[0].tripId)
        assertEquals("T-118KJ7LQW", loads[1].tripId)
        assertEquals(2945.56, loads[0].totalRate, 0.01)
    }

    @Test
    fun parse_skipsSystemMessages() {
        val html = sampleTelegramExport(
            listOf(
                """
                <div class="message default clearfix" id="message1">
                  <div class="body">
                    <div class="from_name">Telegram</div>
                    <div class="text">Alice joined the group</div>
                  </div>
                </div>
                """.trimIndent(),
                loadBlock("T-116KYL6KW"),
            ),
        )
        val loads = parser.parse(html)
        assertEquals(1, loads.size)
        assertEquals("T-116KYL6KW", loads[0].tripId)
    }

    @Test
    fun parse_singleRelayBlockFromTelegramMessage() {
        val html = sampleTelegramExport(listOf(loadBlock("T-116KYL6KW")))
        val loads = parser.parse(html)
        assertEquals(1, loads.size)
        assertEquals("T-116KYL6KW", loads[0].tripId)
    }

    @Test
    fun parse_unicodeBoldTripIdFromRealExportFormat() {
        val html = sampleTelegramExport(listOf(realExportLoadBlock()))
        val loads = parser.parse(html)
        assertTrue(loads.isNotEmpty())
        assertEquals("T-116KYL6KW", loads.first().tripId)
        assertEquals(2945.56, loads.first().totalRate, 1.0)
    }

    private fun realExportLoadBlock(): String {
        val relayText = """
            𝗧𝗿𝗶𝗽 𝗜𝗗:  T-116KYL6KW<br>
            <br>
            PU# 115S1Q2P1 <br><br>Note: Empty trailer<br><br>
            Pu-time: 06/28 03:00 EDT<br>
            Pu-address: SWF2<br>76 Patriot Way<br>Hopewell Junction, NY 12533<br><br>
            Del-time: 06/29 07:00 EDT<br>
            Del-address: TOL3<br>9240 Fremont Pike<br>PERRYSBURG, Ohio 43551<br><br>
            Total Rate: ${'$'}2945.56<br>
            Total Loaded Miles: 1198.03 mi<br>
        """.trimIndent()
        return """
        <div class="message default clearfix" id="message17300">
          <div class="body">
            <div class="from_name">bruce</div>
            <div class="text">$relayText</div>
          </div>
        </div>
        """.trimIndent()
    }

    private fun loadBlock(
        tripId: String,
        rate: String = "2,945.56",
        miles: String = "1,198.03",
    ): String {
        val relayText = """
            Trip ID: $tripId
            Total Rate: ${'$'}$rate
            Total Loaded Miles: $miles mi

            PU# 115S1Q2P1
            Pu-time: 06/28 03:00 EDT
            Pu-address: SWF2, Hopewell Junction, NY

            Del-time: 06/29 07:00 EDT
            Del-address: TOL3, Perrysburg, OH
        """.trimIndent().replace("\n", "<br>\n")
        return """
        <div class="message default clearfix" id="message-${tripId.hashCode()}">
          <div class="body">
            <div class="pull_right date details" title="28.06.2026 14:30:00 UTC+03:00">14:30</div>
            <div class="from_name">Amazon Relay Bot</div>
            <div class="text">
              $relayText
            </div>
          </div>
        </div>
        """.trimIndent()
    }

    private fun sampleTelegramExport(messageBlocks: List<String>): String = """
        <!DOCTYPE html>
        <html>
        <head><title>Exported Data</title></head>
        <body>
          <div class="page_wrap">
            <div class="page_header">Chat export</div>
            <div class="history">
              ${messageBlocks.joinToString("\n")}
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()
}
