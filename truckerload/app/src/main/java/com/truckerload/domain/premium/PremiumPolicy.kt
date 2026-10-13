package com.truckerload.domain.premium

/** What stays behind the paid plan after the first month. The journal itself stays free. */
enum class PremiumFeature {
    TELEGRAM,
    ANALYTICS,
    WEEKLY_GOAL,
    MAP,
    TAX,
    SCANNER,
    CAMERA,
    EXPORT,
    DRIVE,
    DEADHEAD,
}

data class PremiumStatus(
    val subscribed: Boolean,
    val unlocked: Boolean,
) {
    fun allows(@Suppress("UNUSED_PARAMETER") feature: PremiumFeature): Boolean = unlocked

    companion object {
        val LOCKED = PremiumStatus(subscribed = false, unlocked = false)
    }
}

object PremiumPolicy {
    /**
     * Play Console subscription. The free month is a Play offer, not a timer in the app.
     *
     * Product id: [PLAY_PRODUCT_ID]
     * Base plan id: [PLAY_BASE_PLAN_ID] — auto-renewing, every month
     * United States price on that base plan: [US_MONTHLY_PRICE_MICROS] ($2.99)
     * Offer id: [FREE_MONTH_OFFER_ID] — free trial, one month, tag [FREE_MONTH_OFFER_TAG]
     *
     * Play localizes the charge. The button must show the price Play returns.
     */
    const val PLAY_PRODUCT_ID = "premium_monthly_subscription"

    const val PLAY_BASE_PLAN_ID = "monthly"

    /** Tag this offer in Play Console. A zero price alone is not enough. */
    const val FREE_MONTH_OFFER_TAG = "free-month"

    const val FREE_MONTH_OFFER_ID = "free-month"

    /** $2.99 in micros. This is the US catalog price to enter in Play Console. */
    const val US_MONTHLY_PRICE_MICROS = 2_990_000L

    /** Offline cache of a successful Play check. After this, Premium stays off until Play answers. */
    const val SUBSCRIPTION_CACHE_TTL_MS = 72L * 60L * 60L * 1000L

    fun subscriptionManagementUrl(packageName: String): String =
        "https://play.google.com/store/account/subscriptions?sku=$PLAY_PRODUCT_ID&package=$packageName"

    /** An active Play purchase covers both the free-trial offer and the paid month. */
    fun status(subscribed: Boolean): PremiumStatus =
        PremiumStatus(subscribed = subscribed, unlocked = subscribed)
}
