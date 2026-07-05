package com.truckerload.domain.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialBadgesTest {

    @Test
    fun `no badges for new driver`() {
        assertTrue(SocialBadges.compute(0, 0, 0.0).isEmpty())
    }

    @Test
    fun `loads badge at 100 trips`() {
        val badges = SocialBadges.compute(100, 0, 0.0)
        assertEquals(1, badges.size)
        assertEquals("loads_100", badges.first().id)
    }

    @Test
    fun `legend badge at 500 trips`() {
        val badges = SocialBadges.compute(500, 50_000, 100_000.0)
        assertTrue(badges.any { it.id == "legend" })
        assertTrue(badges.any { it.id == "miles_50k" })
        assertTrue(badges.any { it.id == "revenue_100k" })
    }
}
