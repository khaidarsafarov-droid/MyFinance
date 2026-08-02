package com.truckerload.domain.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {

    @Test
    fun disabled_neverActive() {
        assertFalse(QuietHours.isActive(23, 22, 7, enabled = false))
    }

    @Test
    fun overnightWindow_activeLateAndEarly() {
        assertTrue(QuietHours.isActive(23, 22, 7, enabled = true))
        assertTrue(QuietHours.isActive(3, 22, 7, enabled = true))
        assertFalse(QuietHours.isActive(12, 22, 7, enabled = true))
        assertFalse(QuietHours.isActive(21, 22, 7, enabled = true))
    }

    @Test
    fun sameDayWindow_activeInside() {
        assertTrue(QuietHours.isActive(14, 13, 17, enabled = true))
        assertFalse(QuietHours.isActive(12, 13, 17, enabled = true))
        assertFalse(QuietHours.isActive(17, 13, 17, enabled = true))
    }

    @Test
    fun equalStartEnd_meansAlwaysQuiet() {
        assertTrue(QuietHours.isActive(0, 8, 8, enabled = true))
        assertTrue(QuietHours.isActive(15, 8, 8, enabled = true))
    }
}
