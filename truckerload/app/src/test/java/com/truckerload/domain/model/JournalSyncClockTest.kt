package com.truckerload.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalSyncClockTest {

    @Test
    fun bump_usesWallClockWhenNewer() {
        assertEquals(1_000L, JournalSyncClock.bump(previousAddedAt = 10L, now = 1_000L))
    }

    @Test
    fun bump_isMonotonicWhenClockGoesBackwards() {
        assertEquals(51L, JournalSyncClock.bump(previousAddedAt = 50L, now = 10L))
    }

    @Test
    fun bump_alwaysGreaterThanPrevious() {
        val previous = 9_000L
        assertTrue(JournalSyncClock.bump(previous, now = previous) > previous)
    }
}
