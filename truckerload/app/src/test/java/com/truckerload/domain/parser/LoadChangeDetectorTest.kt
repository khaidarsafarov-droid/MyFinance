package com.truckerload.domain.parser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.money.Money

class LoadChangeDetectorTest {

    @Test
    fun `rate change above epsilon is detected`() {
        assertTrue(LoadChangeDetector.isRateChanged(Money.fromDollars(100.0), Money.fromDollars(100.02)))
        assertFalse(LoadChangeDetector.isRateChanged(Money.fromDollars(100.0), Money.fromDollars(100.005)))
    }

    @Test
    fun `significant rate change respects percent threshold`() {
        assertFalse(LoadChangeDetector.isRateChangedSignificant(Money.fromDollars(1000.0), Money.fromDollars(1005.0), 1.0))
        assertTrue(LoadChangeDetector.isRateChangedSignificant(Money.fromDollars(1000.0), Money.fromDollars(1020.0), 1.0))
    }

    @Test
    fun `miles change above 0_1 is detected`() {
        assertTrue(LoadChangeDetector.isMilesChanged(850.0, 850.2))
        assertFalse(LoadChangeDetector.isMilesChanged(850.0, 850.05))
    }

    @Test
    fun `pu time change above one minute is detected`() {
        val base = 1_700_000_000_000L
        assertTrue(LoadChangeDetector.isFirstPuTimeChanged(base, base + 61_000))
        assertFalse(LoadChangeDetector.isFirstPuTimeChanged(base, base + 30_000))
    }
}
