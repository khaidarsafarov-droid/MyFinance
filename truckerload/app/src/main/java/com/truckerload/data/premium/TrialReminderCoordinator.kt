package com.truckerload.data.premium

import android.content.Context
import com.truckerload.domain.premium.PlaySubscriptionOffer
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.domain.premium.TrialPurchaseObservation
import com.truckerload.domain.premium.TrialReminderPolicy
import com.truckerload.domain.premium.TrialWorkAction
import java.time.ZoneId

/**
 * Stores a free-month purchase and keeps the one-time reminder aligned with it.
 * Premium stays whatever Play last reported. This class does not change that.
 */
class TrialReminderCoordinator(context: Context) {
    private val app = context.applicationContext
    private val store = TrialReminderStore(app)

    fun noteOffer(
        offer: PlaySubscriptionOffer,
        launched: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (!isFreeMonthOffer(offer)) return
        val period = offer.phases.firstOrNull { it.priceAmountMicros == 0L }?.billingPeriod
        store.markOfferEligible(nowMillis, period, offer.recurringFormattedPrice)
        if (launched) store.markLaunchedWithFreeMonth(nowMillis)
    }

    fun onAcknowledgedSubscriptions(
        premiumActive: Boolean,
        purchases: List<TrialPurchaseObservation>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        if (!premiumActive) {
            stopReminder()
            return
        }
        val snap = store.read()
        val token = snap.state.purchaseToken
        val sameToken = token.isNotBlank() && purchases.any { it.purchaseToken == token }
        if (sameToken) {
            if (nowMillis < snap.state.trialEndMillis) {
                syncWork(nowMillis)
            } else if (snap.state.scheduledTriggerAtMillis != 0L) {
                TrialEndReminderWorker.cancel(app)
            }
            return
        }
        val trial = purchases
            .filter { TrialReminderPolicy.isFreeMonthTrial(it, snap.signals, nowMillis) }
            .maxByOrNull { it.purchaseTimeMillis }
        if (trial == null) {
            stopReminder()
            return
        }
        val end = TrialReminderPolicy.trialEndMillis(
            trial.purchaseTimeMillis,
            snap.signals.billingPeriod,
            zone,
        )
        store.recordTrial(
            purchaseToken = trial.purchaseToken,
            purchaseTimeMillis = trial.purchaseTimeMillis,
            trialEndMillis = end,
            formattedPrice = snap.signals.formattedPrice,
        )
        syncWork(nowMillis)
    }

    private fun syncWork(nowMillis: Long) {
        val decision = TrialReminderPolicy.decide(
            premiumActive = true,
            state = store.read().state,
            nowMillis = nowMillis,
        )
        when (decision.work) {
            TrialWorkAction.CANCEL -> TrialEndReminderWorker.cancel(app)
            TrialWorkAction.KEEP -> Unit
            TrialWorkAction.ENQUEUE -> {
                if (!TrialEndReminderWorker.canPost(app)) return
                TrialEndReminderWorker.enqueue(app, decision.triggerAtMillis, nowMillis)
                store.markScheduled(decision.triggerAtMillis)
            }
        }
    }

    private fun stopReminder() {
        val state = store.read().state
        val hadWork = state.purchaseToken.isNotBlank() || state.scheduledTriggerAtMillis != 0L
        store.clearActiveTrial()
        if (hadWork) TrialEndReminderWorker.cancel(app)
    }

    private fun isFreeMonthOffer(offer: PlaySubscriptionOffer): Boolean =
        offer.includesFreeMonth ||
            offer.offerId == PremiumPolicy.FREE_MONTH_OFFER_ID ||
            PremiumPolicy.FREE_MONTH_OFFER_TAG in offer.offerTags
}
