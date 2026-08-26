package com.truckerload.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadSharedElementTest {

    @Test
    fun loadSharedBoundsKey_isStableAndDistinctPerLoad() {
        assertEquals("load-bounds-abc", loadSharedBoundsKey("abc"))
        assertEquals(loadSharedBoundsKey("abc"), loadSharedBoundsKey("abc"))
        assertNotEquals(loadSharedBoundsKey("a"), loadSharedBoundsKey("b"))
    }

    @Test
    fun sharedElementEnterExit_respectReduceMotion() {
        assertTrue(motionDurationMs(reduceMotion = true, normalMs = 220) == 0)
        assertTrue(motionDurationMs(reduceMotion = false, normalMs = 220) == 220)
        assertNotNull(navSharedElementEnter(reduceMotion = false))
        assertNotNull(navSharedElementExit(reduceMotion = true))
    }
}
