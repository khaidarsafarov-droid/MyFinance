package com.truckerload.data.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrialReminderStoreTest {
    private lateinit var store: TrialReminderStore

    @Before
    fun setUp() {
        store = TrialReminderStore(RuntimeEnvironment.getApplication())
    }

    @Test
    fun notificationFlag_isOncePerPurchaseToken() {
        store.recordTrial("token-a", 1_000L, 2_000L, "$2.99")
        assertFalse(store.wasNotified("token-a"))
        assertFalse(store.read().state.notificationSent)

        store.markNotified("token-a")
        store.markNotified("token-a")
        assertTrue(store.wasNotified("token-a"))
        assertTrue(store.read().state.notificationSent)

        store.recordTrial("token-b", 3_000L, 4_000L, "$2.99")
        assertFalse(store.wasNotified("token-b"))
        assertFalse(store.read().state.notificationSent)
        assertTrue(store.wasNotified("token-a"))
    }

    @Test
    fun bannerDismiss_doesNotCountAsANotification() {
        store.recordTrial("token-a", 1_000L, 2_000L, "$2.99")
        store.dismissBanner("token-a")
        assertTrue(store.wasBannerDismissed("token-a"))
        assertTrue(store.read().state.bannerDismissed)
        assertFalse(store.wasNotified("token-a"))
        assertFalse(store.read().state.notificationSent)
    }

    @Test
    fun clearActiveTrial_keepsTheOnceOnlyFlag() {
        store.recordTrial("token-a", 1_000L, 2_000L, "$2.99")
        store.markNotified("token-a")
        store.markScheduled(1_500L)
        store.clearActiveTrial()

        assertEquals("", store.read().state.purchaseToken)
        assertEquals(0L, store.read().state.scheduledTriggerAtMillis)
        assertTrue(store.wasNotified("token-a"))
    }

    @Test
    fun offerSignals_roundTripTheBillingPeriodAndPrice() {
        store.markOfferEligible(50L, "P1M", "$2.99")
        store.markLaunchedWithFreeMonth(60L)
        val signals = store.read().signals
        assertEquals(50L, signals.offerEligibleAtMillis)
        assertEquals(60L, signals.launchedWithFreeMonthAtMillis)
        assertEquals("P1M", signals.billingPeriod)
        assertEquals("$2.99", signals.formattedPrice)
    }
}
