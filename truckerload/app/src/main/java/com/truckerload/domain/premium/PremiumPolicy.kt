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
     * Play Console subscription id. The free month is a Play offer on this product
     * (base plan + 1 month free trial), not a timer inside the app.
     */
    const val PLAY_PRODUCT_ID = "premium_monthly_subscription"

    /** An active Play purchase covers both the free-trial offer and the paid month. */
    fun status(subscribed: Boolean): PremiumStatus =
        PremiumStatus(subscribed = subscribed, unlocked = subscribed)
}
