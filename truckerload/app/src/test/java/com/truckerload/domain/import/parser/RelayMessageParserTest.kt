package com.truckerload.domain.import.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelayMessageParserTest {

  private val parser = RelayMessageParser()

  @Test
  fun parse_multipleBlocksFromPromptSample() {
    val sample = """
      Trip ID: T-116KYL6KW
      Total Rate: ${'$'}2,945.56
      Total Loaded Miles: 1,198.03 mi

      PU# 115S1Q2P1
      Pu-time: 06/28 03:00 EDT
      Pu-address: SWF2, Hopewell Junction, NY

      Del-time: 06/29 07:00 EDT
      Del-address: TOL3, Perrysburg, OH
      ---
      Trip ID: T-118KJ7LQW
      Total Rate: ${'$'}1,800.00
      Total Loaded Miles: 850.50 mi

      PU# 116S1Q2P2
      Pu-time: 06/28 05:00 CDT
      Pu-address: MDW2, Chicago, IL

      Del-time: 06/29 09:00 EDT
      Del-address: ATL3, Atlanta, GA
    """.trimIndent()

    val loads = parser.parse(sample)
    assertTrue(loads.size >= 2)
    assertEquals("T-116KYL6KW", loads[0].tripId)
    assertEquals(2945.56, loads[0].totalRate, 0.01)
  }
}
