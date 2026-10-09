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
    val inTrial: Boolean,
    val trialEndsAt: Long,
    val unlocked: Boolean,
) {
    fun allows(@Suppress("UNUSED_PARAMETER") feature: PremiumFeature): Boolean = unlocked

    fun trialDaysLeft(now: Long): Int {
        if (!inTrial) return 0
        val left = (trialEndsAt - now).coerceAtLeast(0L)
        if (left == 0L) return 0
        val day = PremiumPolicy.DAY_MS
        return ((left + day - 1) / day).toInt()
    }

    companion object {
        val LOCKED = PremiumStatus(
            subscribed = false,
            inTrial = false,
            trialEndsAt = 0L,
            unlocked = false,
        )
    }
}

object PremiumPolicy {
    const val TRIAL_DAYS = 30
    const val DAY_MS = 24L * 60L * 60L * 1000L
    const val TRIAL_MS = TRIAL_DAYS * DAY_MS

    /** Play Console subscription id. Create it as a monthly base plan, without a second Play trial. */
    const val PLAY_PRODUCT_ID = "truckorig_premium_monthly"

    fun status(trialStartedAt: Long, subscribed: Boolean, now: Long): PremiumStatus {
        val ends = if (trialStartedAt > 0L) trialStartedAt + TRIAL_MS else 0L
        val inTrial = !subscribed && trialStartedAt > 0L && now < ends
        return PremiumStatus(
            subscribed = subscribed,
            inTrial = inTrial,
            trialEndsAt = ends,
            unlocked = subscribed || inTrial,
        )
    }
}
