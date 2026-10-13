package com.truckerload.domain.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayOfferSelectorTest {

    @Test
    fun taggedFreeMonth_beatsPaidBasePlan() {
        val selected = PlayOfferSelector.select(listOf(paidMonthly(), freeMonth(tag = true)))
        assertEquals("token-free", selected?.offerToken)
        assertTrue(selected!!.includesFreeMonth)
        assertEquals("$2.99", selected.recurringFormattedPrice)
    }

    @Test
    fun offerId_selectsFreeMonthWhenTagIsMissing() {
        val selected = PlayOfferSelector.select(listOf(paidMonthly(), freeMonth(tag = false)))
        assertEquals("token-free", selected?.offerToken)
    }

    @Test
    fun untaggedFreePhase_isStillPreferred() {
        val offer = freeMonth(tag = false).copy(offerId = "trial-1")
        val selected = PlayOfferSelector.select(listOf(paidMonthly(), offer))
        assertEquals("token-free", selected?.offerToken)
    }

    @Test
    fun usedTrial_fallsBackToPaidMonthly() {
        val selected = PlayOfferSelector.select(listOf(yearly(), paidMonthly()))
        assertEquals("token-paid", selected?.offerToken)
        assertFalse(selected!!.includesFreeMonth)
        assertEquals("$2.99", selected.recurringFormattedPrice)
    }

    @Test
    fun emptyCatalog_hasNothingToBuy() {
        assertNull(PlayOfferSelector.select(emptyList()))
    }

    @Test
    fun usCatalogPrice_isTwoDollarsNinetyNine() {
        assertEquals(2_990_000L, PremiumPolicy.US_MONTHLY_PRICE_MICROS)
        assertEquals(
            "https://play.google.com/store/account/subscriptions?sku=premium_monthly_subscription&package=com.truckorig",
            PremiumPolicy.subscriptionManagementUrl("com.truckorig"),
        )
    }

    private fun paidMonthly() = PlaySubscriptionOffer(
        offerToken = "token-paid",
        offerId = null,
        basePlanId = PremiumPolicy.PLAY_BASE_PLAN_ID,
        offerTags = emptySet(),
        phases = listOf(paidPhase()),
    )

    private fun yearly() = PlaySubscriptionOffer(
        offerToken = "token-year",
        offerId = null,
        basePlanId = "yearly",
        offerTags = emptySet(),
        phases = listOf(paidPhase().copy(formattedPrice = "$29.99", billingPeriod = "P1Y")),
    )

    private fun freeMonth(tag: Boolean) = PlaySubscriptionOffer(
        offerToken = "token-free",
        offerId = PremiumPolicy.FREE_MONTH_OFFER_ID,
        basePlanId = PremiumPolicy.PLAY_BASE_PLAN_ID,
        offerTags = if (tag) setOf(PremiumPolicy.FREE_MONTH_OFFER_TAG) else emptySet(),
        phases = listOf(
            PlayPricingPhase(0L, "P1M", 1, "Free"),
            paidPhase(),
        ),
    )

    private fun paidPhase() = PlayPricingPhase(
        priceAmountMicros = PremiumPolicy.US_MONTHLY_PRICE_MICROS,
        billingPeriod = "P1M",
        billingCycleCount = 0,
        formattedPrice = "$2.99",
    )
}
