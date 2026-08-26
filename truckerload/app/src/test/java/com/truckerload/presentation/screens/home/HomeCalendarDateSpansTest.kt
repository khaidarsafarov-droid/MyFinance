package com.truckerload.presentation.screens.home

import com.truckerload.data.local.entities.LoadDateSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCalendarDateSpansTest {

    @Test
    fun toDateKeys_includesStartWhenEndMissing() {
        val keys = HomeCalendarDateSpans.toDateKeys(
            listOf(LoadDateSpan(startDate = "2026-08-01", endDate = null)),
        )
        assertEquals(setOf("2026-08-01"), keys)
    }

    @Test
    fun toDateKeys_expandsInclusiveRange() {
        val keys = HomeCalendarDateSpans.toDateKeys(
            listOf(LoadDateSpan(startDate = "2026-08-01", endDate = "2026-08-03")),
        )
        assertEquals(setOf("2026-08-01", "2026-08-02", "2026-08-03"), keys)
    }

    @Test
    fun toDateKeys_capsLongSpans() {
        val keys = HomeCalendarDateSpans.toDateKeys(
            listOf(LoadDateSpan(startDate = "2026-01-01", endDate = "2026-12-31")),
        )
        assertEquals(60, keys.size)
        assertTrue(keys.contains("2026-01-01"))
    }
}
