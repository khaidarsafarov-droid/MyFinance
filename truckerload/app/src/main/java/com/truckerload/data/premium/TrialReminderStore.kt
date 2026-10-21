package com.truckerload.data.premium

import android.content.Context
import com.truckerload.domain.premium.TrialOfferSignals
import com.truckerload.domain.premium.TrialReminderState

data class TrialReminderSnapshot(
    val signals: TrialOfferSignals,
    val state: TrialReminderState,
)

/**
 * Local prefs for the free-month reminder.
 *
 * The purchase token only keys the once-only notification and the dismissed banner.
 * It does not unlock Premium. There is no server copy.
 */
class TrialReminderStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): TrialReminderSnapshot {
        val token = prefs.getString(KEY_TOKEN, "").orEmpty()
        return TrialReminderSnapshot(
            signals = TrialOfferSignals(
                offerEligibleAtMillis = prefs.getLong(KEY_ELIGIBLE_AT, 0L),
                launchedWithFreeMonthAtMillis = prefs.getLong(KEY_LAUNCHED_AT, 0L),
                billingPeriod = prefs.getString(KEY_PERIOD, null)?.takeIf { it.isNotBlank() },
                formattedPrice = prefs.getString(KEY_PRICE, null)?.takeIf { it.isNotBlank() },
            ),
            state = TrialReminderState(
                purchaseToken = token,
                purchaseTimeMillis = prefs.getLong(KEY_PURCHASE_TIME, 0L),
                trialEndMillis = prefs.getLong(KEY_TRIAL_END, 0L),
                formattedPrice = prefs.getString(KEY_PRICE, null)?.takeIf { it.isNotBlank() },
                bannerDismissed = token.isNotBlank() && token in stringSet(KEY_DISMISSED),
                notificationSent = token.isNotBlank() && token in stringSet(KEY_NOTIFIED),
                scheduledTriggerAtMillis = prefs.getLong(KEY_SCHEDULED, 0L),
            ),
        )
    }

    fun markOfferEligible(atMillis: Long, billingPeriod: String?, formattedPrice: String?) {
        val editor = prefs.edit().putLong(KEY_ELIGIBLE_AT, atMillis)
        if (!billingPeriod.isNullOrBlank()) editor.putString(KEY_PERIOD, billingPeriod)
        if (!formattedPrice.isNullOrBlank()) editor.putString(KEY_PRICE, formattedPrice)
        editor.apply()
    }

    fun markLaunchedWithFreeMonth(atMillis: Long) {
        prefs.edit().putLong(KEY_LAUNCHED_AT, atMillis).apply()
    }

    fun recordTrial(
        purchaseToken: String,
        purchaseTimeMillis: Long,
        trialEndMillis: Long,
        formattedPrice: String?,
    ) {
        if (purchaseToken.isBlank()) return
        val previousToken = prefs.getString(KEY_TOKEN, "").orEmpty()
        val previousEnd = prefs.getLong(KEY_TRIAL_END, 0L)
        val editor = prefs.edit()
            .putString(KEY_TOKEN, purchaseToken)
            .putLong(KEY_PURCHASE_TIME, purchaseTimeMillis)
            .putLong(KEY_TRIAL_END, trialEndMillis)
        if (!formattedPrice.isNullOrBlank()) editor.putString(KEY_PRICE, formattedPrice)
        if (previousToken != purchaseToken || previousEnd != trialEndMillis) {
            editor.putLong(KEY_SCHEDULED, 0L)
        }
        editor.apply()
    }

    /** Once per purchase token. A later purchase token can notify again. */
    fun markNotified(purchaseToken: String) {
        if (purchaseToken.isBlank()) return
        putToken(KEY_NOTIFIED, purchaseToken)
    }

    fun dismissBanner(purchaseToken: String) {
        if (purchaseToken.isBlank()) return
        putToken(KEY_DISMISSED, purchaseToken)
    }

    fun wasNotified(purchaseToken: String): Boolean =
        purchaseToken.isNotBlank() && purchaseToken in stringSet(KEY_NOTIFIED)

    fun wasBannerDismissed(purchaseToken: String): Boolean =
        purchaseToken.isNotBlank() && purchaseToken in stringSet(KEY_DISMISSED)

    fun markScheduled(triggerAtMillis: Long) {
        prefs.edit().putLong(KEY_SCHEDULED, triggerAtMillis).apply()
    }

    fun clearSchedule() {
        prefs.edit().putLong(KEY_SCHEDULED, 0L).apply()
    }

    /** Drops the active trial. Once-only flags for past tokens stay. */
    fun clearActiveTrial() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_PURCHASE_TIME)
            .remove(KEY_TRIAL_END)
            .putLong(KEY_SCHEDULED, 0L)
            .apply()
    }

    private fun putToken(key: String, token: String) {
        val next = stringSet(key).toMutableSet()
        next.add(token)
        prefs.edit().putStringSet(key, next).apply()
    }

    private fun stringSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet()).orEmpty().toSet()

    private companion object {
        const val PREFS = "truckorig_trial_reminder"
        const val KEY_ELIGIBLE_AT = "offer_eligible_at"
        const val KEY_LAUNCHED_AT = "launched_with_free_month_at"
        const val KEY_PERIOD = "billing_period"
        const val KEY_PRICE = "formatted_price"
        const val KEY_TOKEN = "purchase_token"
        const val KEY_PURCHASE_TIME = "purchase_time"
        const val KEY_TRIAL_END = "trial_end"
        const val KEY_SCHEDULED = "scheduled_trigger_at"
        const val KEY_NOTIFIED = "notified_tokens"
        const val KEY_DISMISSED = "dismissed_tokens"
    }
}
