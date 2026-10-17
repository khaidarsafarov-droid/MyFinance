package com.truckerload.presentation.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadArrivalTest {

    @Test
    fun onlyALoadSavedJustNowSlidesIn() {
        val now = 1_700_000_000_000L
        assertTrue(LoadArrival.shouldPlay(now - 200L, now))
        assertFalse(LoadArrival.shouldPlay(now - LoadArrival.WINDOW_MS - 1L, now))
        assertFalse(LoadArrival.shouldPlay(0L, now))
        assertFalse(LoadArrival.shouldPlay(now + 5_000L, now))
    }
}
