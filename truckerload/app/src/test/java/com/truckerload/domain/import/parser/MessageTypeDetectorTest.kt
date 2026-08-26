package com.truckerload.domain.import.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTypeDetectorTest {

  private val detector = MessageTypeDetector

  @Test
  fun detect_relayText() {
    val sample = """
      Trip ID: T-116KYL6KW
      Total Rate: ${'$'}2,945.56
      Total Loaded Miles: 1,198.03 mi
      PU# 115S1Q2P1
      Pu-address: Hopewell Junction, NY
    """.trimIndent()
    assertEquals(ImportMessageType.RELAY_TEXT, detector.detect(sample))
  }

  @Test
    fun detect_html() {
    val html = "<html><body><table><tr><th>Trip</th></tr></table></body></html>"
    assertEquals(ImportMessageType.HTML, detector.detect(html))
  }

    @Test
    fun detect_csvBeforeHtmlishDivInCell() {
        val csv = """
            tripId,rate,miles,origin,destination,date
            T-1,2500,100,"<div>Austin, TX</div>",Dallas TX,2026-03-01
        """.trimIndent()
        assertEquals(ImportMessageType.CSV, detector.detect(csv))
    }

    @Test
    fun detect_exportText() {
        val line = "29.06.2026 | TOL3 → RDU1 | 1,198 mi | $2,945.56"
        assertEquals(ImportMessageType.EXPORT_TEXT, detector.detect(line))
    }

    @Test
    fun detect_telegramJsonExportBeforeRelayMarkersInPayload() {
        val json = """
            {
              "name": "Relay",
              "type": "personal_chat",
              "id": 1,
              "messages": [
                {
                  "id": 1,
                  "type": "message",
                  "date": "2025-07-05T10:00:00",
                  "text": "Trip ID: T-AAA\nTotal Rate: ${'$'}1000.00\nTotal Loaded Miles: 400 mi"
                }
              ]
            }
        """.trimIndent()
        assertEquals(ImportMessageType.TELEGRAM_JSON, detector.detect(json))
    }
}
