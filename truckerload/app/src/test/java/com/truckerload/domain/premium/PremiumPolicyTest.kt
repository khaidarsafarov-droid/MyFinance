package com.truckerload.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPolicyTest {

    @Test
    fun firstMonth_unlocksPremiumFeatures() {
        val start = 1_000L
        val status = PremiumPolicy.status(start, subscribed = false, now = start + PremiumPolicy.DAY_MS)
        assertTrue(status.inTrial)
        assertTrue(status.unlocked)
        assertTrue(status.allows(PremiumFeature.TELEGRAM))
        assertEquals(29, status.trialDaysLeft(start + PremiumPolicy.DAY_MS))
    }

    @Test
    fun afterTrial_locksUntilSubscribed() {
        val start = 1_000L
        val later = start + PremiumPolicy.TRIAL_MS
        val locked = PremiumPolicy.status(start, subscribed = false, now = later)
        assertFalse(locked.inTrial)
        assertFalse(locked.unlocked)
        assertFalse(locked.allows(PremiumFeature.DEADHEAD))

        val paid = PremiumPolicy.status(start, subscribed = true, now = later)
        assertTrue(paid.subscribed)
        assertTrue(paid.unlocked)
        assertFalse(paid.inTrial)
    }
}
