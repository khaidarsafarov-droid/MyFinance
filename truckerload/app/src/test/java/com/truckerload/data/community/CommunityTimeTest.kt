package com.truckerload.data.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityTimeTest {

    @Test
    fun parseMillis_blankAndNull_areZero() {
        assertEquals(0L, CommunityTime.parseMillis(""))
        assertEquals(0L, CommunityTime.parseMillis("   "))
        assertEquals(0L, CommunityTime.parseMillis("null"))
        assertEquals(0L, CommunityTime.parseMillis("NULL"))
    }

    @Test
    fun parseMillis_isoInstant() {
        val millis = CommunityTime.parseMillis("2026-08-19T03:00:00Z")
        assertEquals(CommunityTime.parseMillis("2026-08-19T03:00:00+00:00"), millis)
        assertTrue(millis > 1_700_000_000_000L)
    }

    @Test
    fun parseMillis_postgresSpaceSeparatedOffset() {
        val millis = CommunityTime.parseMillis("2026-08-19 03:00:00+00")
        assertEquals(CommunityTime.parseMillis("2026-08-19T03:00:00Z"), millis)
    }

    @Test
    fun parseMillis_fractionalPostgresOffset() {
        val millis = CommunityTime.parseMillis("2026-08-19T03:00:00.123456+00")
        assertEquals(CommunityTime.parseMillis("2026-08-19T03:00:00.123456Z"), millis)
    }

    @Test
    fun parseMillis_epochSecondsAndMillis() {
        assertEquals(1_724_000_000_000L, CommunityTime.parseMillis("1724000000"))
        assertEquals(1_724_000_000_000L, CommunityTime.parseMillis("1724000000000"))
    }

    @Test
    fun toIso_roundTripsThroughParse() {
        val millis = 1_724_000_000_000L
        assertEquals(millis, CommunityTime.parseMillis(CommunityTime.toIso(millis)))
    }
}
