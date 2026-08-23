package com.truckerload.domain.crowd

import org.junit.Assert.assertTrue
import org.junit.Test

class CrowdWeekWindowTest {

    @Test
    fun currentWindowCoversNow() {
        val window = CrowdWeekWindow.current()
        val now = System.currentTimeMillis()
        assertTrue(window.week in 1..53)
        assertTrue(window.year >= 2020)
        assertTrue(window.startMillis <= now)
        assertTrue(window.endMillis >= now)
        assertTrue(window.endMillis > window.startMillis)
    }
}
