package com.truckerload.domain.premium

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Local estimate of when the Play free month ends, and whether to remind once.
 *
 * Assumption: there is no server, and Play Billing does not send a trial-end timestamp.
 * The free month is offer [PremiumPolicy.FREE_MONTH_OFFER_ID] (tag
 * [PremiumPolicy.FREE_MONTH_OFFER_TAG]) on [PremiumPolicy.PLAY_PRODUCT_ID], base plan
 * [PremiumPolicy.PLAY_BASE_PLAN_ID]. Trial end is purchaseTime plus that offer's free-phase
 * billing period when ProductDetails has been seen (P1M is one calendar month in [zone]).
 * If the period is missing, trial end is purchaseTime plus 30 days. This reminder does not
 * unlock or lock Premium. An active Play purchase remains the only access check.
 */
data class TrialPurchaseObservation(
    val purchaseToken: String,
    val purchaseTimeMillis: Long,
    val originalJson: String,
)

/** What this device has seen of the free-month offer. Not a Play entitlement. */
data class TrialOfferSignals(
    val offerEligibleAtMillis: Long = 0L,
    val launchedWithFreeMonthAtMillis: Long = 0L,
    val billingPeriod: String? = null,
    val formattedPrice: String? = null,
)

data class TrialReminderState(
    val purchaseToken: String = "",
    val purchaseTimeMillis: Long = 0L,
    val trialEndMillis: Long = 0L,
    val formattedPrice: String? = null,
    val bannerDismissed: Boolean = false,
    val notificationSent: Boolean = false,
    val scheduledTriggerAtMillis: Long = 0L,
)

enum class TrialWorkAction {
    CANCEL,
    ENQUEUE,
    KEEP,
}

data class TrialReminderDecision(
    val onTrial: Boolean,
    val showBanner: Boolean,
    val postNotification: Boolean,
    val work: TrialWorkAction,
    val triggerAtMillis: Long,
)

object TrialReminderPolicy {
    const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    const val REMINDER_LEAD_MS = 3L * 24L * 60L * 60L * 1000L

    /** Device clock versus Play purchaseTime, including eligibility stored just after the buy. */
    private const val CLOCK_SKEW_MS = 60L * 60L * 1000L
    private const val LAUNCH_MATCH_MS = 24L * 60L * 60L * 1000L

    private val OFFER_ID_IN_JSON = Regex(
        """"offerId"\s*:\s*"${Regex.escape(PremiumPolicy.FREE_MONTH_OFFER_ID)}"""",
    )
    private val OFFER_TAG_IN_JSON = Regex(
        """"offerTags"\s*:\s*\[[^\]]*"${Regex.escape(PremiumPolicy.FREE_MONTH_OFFER_TAG)}"[^\]]*\]""",
    )
    private val ISO_PERIOD = Regex(
        """^P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?$""",
        RegexOption.IGNORE_CASE,
    )

    fun trialEndMillis(
        purchaseTimeMillis: Long,
        billingPeriod: String?,
        zone: ZoneId = ZoneOffset.UTC,
    ): Long {
        val fromOffer = billingPeriod?.let { addBillingPeriod(purchaseTimeMillis, it, zone) }
        return fromOffer ?: purchaseTimeMillis + THIRTY_DAYS_MS
    }

    /**
     * A purchase counts as the free month when it was bought with that offer, or when it is
     * still inside the first 30 days and this device saw the user was eligible for the offer.
     */
    fun isFreeMonthTrial(
        observation: TrialPurchaseObservation,
        signals: TrialOfferSignals,
        nowMillis: Long,
    ): Boolean {
        if (observation.purchaseToken.isBlank() || observation.purchaseTimeMillis <= 0L) return false
        if (boughtWithFreeMonthOffer(observation, signals)) return true
        if (!withinFirst30Days(observation.purchaseTimeMillis, nowMillis)) return false
        return offerWasEligible(signals.offerEligibleAtMillis, observation.purchaseTimeMillis)
    }

    /** Inclusive of the moment three days before [trialEndMillis], exclusive of the end. */
    fun inReminderWindow(nowMillis: Long, trialEndMillis: Long): Boolean {
        if (trialEndMillis <= 0L) return false
        val opensAt = trialEndMillis - REMINDER_LEAD_MS
        return nowMillis >= opensAt && nowMillis < trialEndMillis
    }

    fun decide(
        premiumActive: Boolean,
        state: TrialReminderState,
        nowMillis: Long,
    ): TrialReminderDecision {
        val hasRecord = state.purchaseToken.isNotBlank() && state.trialEndMillis > 0L
        val onTrial = premiumActive && hasRecord && nowMillis < state.trialEndMillis
        val inWindow = onTrial && inReminderWindow(nowMillis, state.trialEndMillis)
        val trigger = if (hasRecord) state.trialEndMillis - REMINDER_LEAD_MS else 0L
        val work = when {
            !onTrial || state.notificationSent -> TrialWorkAction.CANCEL
            trigger > 0L && state.scheduledTriggerAtMillis == trigger -> TrialWorkAction.KEEP
            else -> TrialWorkAction.ENQUEUE
        }
        return TrialReminderDecision(
            onTrial = onTrial,
            showBanner = inWindow && !state.bannerDismissed,
            postNotification = inWindow && !state.notificationSent,
            work = work,
            triggerAtMillis = trigger,
        )
    }

    fun endDate(trialEndMillis: Long, locale: Locale, zone: ZoneId): String =
        Instant.ofEpochMilli(trialEndMillis)
            .atZone(zone)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun boughtWithFreeMonthOffer(
        observation: TrialPurchaseObservation,
        signals: TrialOfferSignals,
    ): Boolean {
        if (jsonIndicatesFreeMonth(observation.originalJson)) return true
        val launchedAt = signals.launchedWithFreeMonthAtMillis
        if (launchedAt <= 0L) return false
        val delta = observation.purchaseTimeMillis - launchedAt
        return delta >= -CLOCK_SKEW_MS && delta <= LAUNCH_MATCH_MS
    }

    fun offerWasEligible(offerEligibleAtMillis: Long, purchaseTimeMillis: Long): Boolean {
        if (offerEligibleAtMillis <= 0L) return false
        val delta = purchaseTimeMillis - offerEligibleAtMillis
        return delta >= -CLOCK_SKEW_MS && delta <= THIRTY_DAYS_MS
    }

    fun withinFirst30Days(purchaseTimeMillis: Long, nowMillis: Long): Boolean {
        val age = nowMillis - purchaseTimeMillis
        return age >= -CLOCK_SKEW_MS && age <= THIRTY_DAYS_MS
    }

    /** Play's purchase JSON sometimes echoes the offer id or tags. The token alone does not. */
    fun jsonIndicatesFreeMonth(originalJson: String): Boolean {
        if (originalJson.isBlank()) return false
        return OFFER_ID_IN_JSON.containsMatchIn(originalJson) ||
            OFFER_TAG_IN_JSON.containsMatchIn(originalJson)
    }

    private fun addBillingPeriod(startMillis: Long, period: String, zone: ZoneId): Long? {
        val match = ISO_PERIOD.matchEntire(period.trim()) ?: return null
        val years = match.groupValues[1].toLongOrNull() ?: 0L
        val months = match.groupValues[2].toLongOrNull() ?: 0L
        val weeks = match.groupValues[3].toLongOrNull() ?: 0L
        val days = match.groupValues[4].toLongOrNull() ?: 0L
        if (years == 0L && months == 0L && weeks == 0L && days == 0L) return null
        return Instant.ofEpochMilli(startMillis)
            .atZone(zone)
            .plusYears(years)
            .plusMonths(months)
            .plusWeeks(weeks)
            .plusDays(days)
            .toInstant()
            .toEpochMilli()
    }
}
