package com.truckerload.domain.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPolicyTest {

    @Test
    fun playSubscription_unlocksPremium() {
        val status = PremiumPolicy.status(subscribed = true)
        assertTrue(status.subscribed)
        assertTrue(status.unlocked)
        assertTrue(status.allows(PremiumFeature.TELEGRAM))
    }

    @Test
    fun firstOpen_asksToSubscribeUntilHandledOrPurchased() {
        assertTrue(PremiumPolicy.shouldPromptForSubscription(subscribed = false, offerAlreadyHandled = false))
        assertFalse(PremiumPolicy.shouldPromptForSubscription(subscribed = true, offerAlreadyHandled = false))
        assertFalse(PremiumPolicy.shouldPromptForSubscription(subscribed = false, offerAlreadyHandled = true))
    }

    @Test
    fun withoutPlaySubscription_featuresStayLocked() {
        val status = PremiumPolicy.status(subscribed = false)
        assertFalse(status.unlocked)
        assertFalse(status.allows(PremiumFeature.EXPORT))
    }
}
