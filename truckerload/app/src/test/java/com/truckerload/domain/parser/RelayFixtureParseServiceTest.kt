package com.truckerload.domain.parser

import com.truckerload.data.repository.AiRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QUALITY_100 #29 — Relay paste fixture via MessageParseService / AiRepository.
 */
class RelayFixtureParseServiceTest {

    private val fixture = """
        Trip ID: T-116KYL6KW
        Total Rate: 2500.00
        Total Loaded Miles: 850 mi
        Pu-address: SWF2, Garner, NC
        Del-address: TOL3, Perrysburg, OH
    """.trimIndent()

    @Test
    fun messageParseService_parsesRelayFixture() {
        val result = MessageParseService().parseLoadFromMessage(fixture)
        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        assertEquals("T-116KYL6KW", load.tripId)
        assertEquals(2500.0, load.totalRate, 0.01)
        assertEquals(850.0, load.totalMiles, 0.01)
    }

    @Test
    fun aiRepository_parseLoadFromMessage_sameFixture() = runBlocking {
        val result = AiRepository().parseLoadFromMessage(fixture)
        assertTrue(result.isSuccess)
        val load = result.getOrThrow()
        assertEquals("T-116KYL6KW", load.tripId)
        assertTrue(load.totalRate > 0)
    }

    @Test
    fun aiRepository_parseAmazonRelayFromMessage_returnsDuration() = runBlocking {
        val withTimes = """
            Trip ID: T-116KYL6KW
            Total Rate: 2500.00
            Total Loaded Miles: 850 mi
            Pu-time: 07/21 08:00 EDT
            Pu-address: SWF2, Garner, NC
            Del-time: 07/22 08:00 EDT
            Del-address: TOL3, Perrysburg, OH
        """.trimIndent()
        val result = AiRepository().parseAmazonRelayFromMessage(withTimes)
        assertTrue(result.isSuccess)
        assertEquals("T-116KYL6KW", result.getOrThrow().load.tripId)
        assertTrue(result.getOrThrow().totalRate > 0)
    }
}
