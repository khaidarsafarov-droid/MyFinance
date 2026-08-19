package com.truckerload.domain.social

import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityWeekWindowTest {

    @Test
    fun currentWindowCoversNow() {
        val window = CommunityWeekWindow.current()
        val now = System.currentTimeMillis()
        assertTrue(window.week in 1..53)
        assertTrue(window.year >= 2020)
        assertTrue(window.startMillis <= now)
        assertTrue(window.endMillis >= now)
        assertTrue(window.endMillis > window.startMillis)
    }
}
