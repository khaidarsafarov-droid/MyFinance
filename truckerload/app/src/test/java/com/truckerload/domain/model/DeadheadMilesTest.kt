package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeadheadMilesTest {

    @Test
    fun drivenMiles_addsDeadheadOnTopOfLoadedMiles() {
        val load = sample(totalMiles = 1000.0, deadheadMiles = 100.0)
        assertEquals(1100.0, load.drivenMiles, 0.01)
    }

    @Test
    fun drivenMiles_ignoresNegativeDeadhead() {
        val load = sample(totalMiles = 800.0, deadheadMiles = -20.0)
        assertEquals(800.0, load.drivenMiles, 0.01)
    }

    @Test
    fun parse_acceptsBlankAsClearAndPlainMiles() {
        assertEquals(0.0, DeadheadMiles.parse("")!!, 0.01)
        assertEquals(100.0, DeadheadMiles.parse("100")!!, 0.01)
        assertEquals(12.5, DeadheadMiles.parse("12,5")!!, 0.01)
    }

    @Test
    fun parse_rejectsJunkAndOutOfRange() {
        assertNull(DeadheadMiles.parse("abc"))
        assertNull(DeadheadMiles.parse("-5"))
        assertNull(DeadheadMiles.parse("10000"))
    }

    private fun sample(totalMiles: Double, deadheadMiles: Double) = Load(
        id = "1",
        tripId = "T-1",
        date = "2026-09-28",
        totalRate = 2500.0,
        totalMiles = totalMiles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 40,
        year = 2026,
        rawMessage = "",
        parsedAt = 0L,
        updatedAt = 0L,
        deadheadMiles = deadheadMiles,
    )
}
