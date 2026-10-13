package com.truckerload.domain.premium

/** One pricing phase from a Play subscription offer. */
data class PlayPricingPhase(
    val priceAmountMicros: Long,
    val billingPeriod: String,
    val billingCycleCount: Int,
    val formattedPrice: String,
)

/** Offer the billing sheet can launch. Mapped from Play [ProductDetails] at the edge. */
data class PlaySubscriptionOffer(
    val offerToken: String,
    val offerId: String?,
    val basePlanId: String,
    val offerTags: Set<String>,
    val phases: List<PlayPricingPhase>,
) {
    /** First phase is a free month, and a later phase is the paid renewal. */
    val includesFreeMonth: Boolean
        get() {
            val first = phases.firstOrNull() ?: return false
            return first.priceAmountMicros == 0L &&
                first.billingPeriod == MONTH &&
                phases.any { it.priceAmountMicros > 0L }
        }

    /** Recurring price Play will charge after the free phase. Null if Play sent no paid phase. */
    val recurringFormattedPrice: String?
        get() = phases.lastOrNull { it.priceAmountMicros > 0L }?.formattedPrice?.takeIf { it.isNotBlank() }

    private companion object {
        const val MONTH = "P1M"
    }
}

/** What the purchase button is allowed to show. Price text always comes from Play. */
data class PremiumOfferQuote(
    val formattedPrice: String?,
    val includesFreeMonth: Boolean,
)

/**
 * Picks the offer to buy.
 *
 * Eligible new subscribers get the free month. Someone who already used the trial
 * only receives the paid base plan from Play, and that plan is what we launch.
 */
object PlayOfferSelector {
    fun select(offers: List<PlaySubscriptionOffer>): PlaySubscriptionOffer? {
        if (offers.isEmpty()) return null
        val monthly = offers.filter { it.basePlanId == PremiumPolicy.PLAY_BASE_PLAN_ID }
        val pool = monthly.ifEmpty { offers }
        return pool.firstOrNull { PremiumPolicy.FREE_MONTH_OFFER_TAG in it.offerTags }
            ?: pool.firstOrNull { it.offerId == PremiumPolicy.FREE_MONTH_OFFER_ID }
            ?: pool.firstOrNull { it.includesFreeMonth }
            ?: pool.firstOrNull { it.offerId.isNullOrBlank() }
            ?: pool.first()
    }
}
