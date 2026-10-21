package com.truckerload.domain.premium

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrialReminderPolicyTest {
    private val day = 24L * 60L * 60L * 1000L
    private val trialEnd = 1_700_000_000_000L
    private val purchaseTime = trialEnd - TrialReminderPolicy.THIRTY_DAYS_MS

    @Test
    fun trialEnd_usesThirtyDaysWhenPlaySentNoPeriod() {
        assertEquals(
            purchaseTime + TrialReminderPolicy.THIRTY_DAYS_MS,
            TrialReminderPolicy.trialEndMillis(purchaseTime, null),
        )
        assertEquals(
            purchaseTime + TrialReminderPolicy.THIRTY_DAYS_MS,
            TrialReminderPolicy.trialEndMillis(purchaseTime, "not-a-period"),
        )
    }

    @Test
    fun trialEnd_usesFreePhaseMonthWhenPlaySentP1M() {
        val start = Instant.parse("2026-01-31T00:00:00Z").toEpochMilli()
        val end = TrialReminderPolicy.trialEndMillis(start, "P1M", ZoneOffset.UTC)
        assertEquals(Instant.parse("2026-02-28T00:00:00Z").toEpochMilli(), end)
        assertTrue(end != start + TrialReminderPolicy.THIRTY_DAYS_MS)
    }

    @Test
    fun reminderWindow_isTheThreeDaysBeforeTrialEnd() {
        val opens = trialEnd - TrialReminderPolicy.REMINDER_LEAD_MS
        assertFalse(TrialReminderPolicy.inReminderWindow(opens - 1L, trialEnd))
        assertTrue(TrialReminderPolicy.inReminderWindow(opens, trialEnd))
        assertTrue(TrialReminderPolicy.inReminderWindow(trialEnd - 1L, trialEnd))
        assertFalse(TrialReminderPolicy.inReminderWindow(trialEnd, trialEnd))
    }

    @Test
    fun banner_showsOnlyInsideTheWindowWhilePremiumIsActive() {
        val inside = state()
        val shown = TrialReminderPolicy.decide(premiumActive = true, inside, trialEnd - day)
        assertTrue(shown.onTrial)
        assertTrue(shown.showBanner)
        assertTrue(shown.postNotification)

        val tooEarly = TrialReminderPolicy.decide(
            premiumActive = true,
            state(),
            trialEnd - TrialReminderPolicy.REMINDER_LEAD_MS - 1L,
        )
        assertTrue(tooEarly.onTrial)
        assertFalse(tooEarly.showBanner)
        assertEquals(TrialWorkAction.ENQUEUE, tooEarly.work)

        val inactive = TrialReminderPolicy.decide(premiumActive = false, inside, trialEnd - day)
        assertFalse(inactive.onTrial)
        assertFalse(inactive.showBanner)
        assertFalse(inactive.postNotification)
        assertEquals(TrialWorkAction.CANCEL, inactive.work)
    }

    @Test
    fun banner_staysHiddenAfterDismiss_notificationIsSeparate() {
        val now = trialEnd - day
        val dismissed = state().copy(bannerDismissed = true)
        val decision = TrialReminderPolicy.decide(premiumActive = true, dismissed, now)
        assertFalse(decision.showBanner)
        assertTrue(decision.postNotification)
    }

    @Test
    fun notification_isOncePerPurchase() {
        val now = trialEnd - day
        val first = TrialReminderPolicy.decide(premiumActive = true, state(), now)
        assertTrue(first.postNotification)
        val again = TrialReminderPolicy.decide(
            premiumActive = true,
            state().copy(notificationSent = true),
            now,
        )
        assertFalse(again.postNotification)
        assertTrue(again.showBanner)
        assertEquals(TrialWorkAction.CANCEL, again.work)
    }

    @Test
    fun work_isCanceledWhenTheTrialIsOverOrMissing() {
        val expired = TrialReminderPolicy.decide(
            premiumActive = true,
            state(),
            trialEnd,
        )
        assertFalse(expired.onTrial)
        assertEquals(TrialWorkAction.CANCEL, expired.work)

        val missing = TrialReminderPolicy.decide(
            premiumActive = true,
            TrialReminderState(),
            trialEnd - day,
        )
        assertEquals(TrialWorkAction.CANCEL, missing.work)
    }

    @Test
    fun work_staysQueuedWhenTheTriggerIsUnchanged() {
        val trigger = trialEnd - TrialReminderPolicy.REMINDER_LEAD_MS
        val decision = TrialReminderPolicy.decide(
            premiumActive = true,
            state().copy(scheduledTriggerAtMillis = trigger),
            trialEnd - 10 * day,
        )
        assertEquals(TrialWorkAction.KEEP, decision.work)
        assertEquals(trigger, decision.triggerAtMillis)
    }

    @Test
    fun freeMonth_whenPurchaseJsonNamesTheOffer() {
        val purchase = observation(
            json = """{"offerId":"free-month","productId":"premium_monthly_subscription"}""",
        )
        assertTrue(
            TrialReminderPolicy.isFreeMonthTrial(purchase, TrialOfferSignals(), purchase.purchaseTimeMillis),
        )
        val tagged = observation(json = """{"offerTags":["intro","free-month"]}""")
        assertTrue(TrialReminderPolicy.jsonIndicatesFreeMonth(tagged.originalJson))
        assertFalse(TrialReminderPolicy.jsonIndicatesFreeMonth("""{"note":"free-month later"}"""))
    }

    @Test
    fun freeMonth_whenLaunchUsedTheOffer() {
        val purchase = observation(json = "{}")
        val signals = TrialOfferSignals(launchedWithFreeMonthAtMillis = purchase.purchaseTimeMillis - 60_000L)
        assertTrue(TrialReminderPolicy.boughtWithFreeMonthOffer(purchase, signals))
        val stale = signals.copy(launchedWithFreeMonthAtMillis = purchase.purchaseTimeMillis - 2 * day)
        assertFalse(TrialReminderPolicy.boughtWithFreeMonthOffer(purchase, stale))
    }

    @Test
    fun freeMonth_withinThirtyDaysOnlyIfTheOfferWasEligible() {
        val purchase = observation(json = "{}")
        val now = purchase.purchaseTimeMillis + 10 * day
        val eligible = TrialOfferSignals(offerEligibleAtMillis = purchase.purchaseTimeMillis - day)
        assertTrue(TrialReminderPolicy.isFreeMonthTrial(purchase, eligible, now))

        val neverEligible = TrialOfferSignals()
        assertFalse(TrialReminderPolicy.isFreeMonthTrial(purchase, neverEligible, now))

        val tooLate = purchase.purchaseTimeMillis + TrialReminderPolicy.THIRTY_DAYS_MS + day
        assertFalse(TrialReminderPolicy.isFreeMonthTrial(purchase, eligible, tooLate))

        val eligibleTooLongAgo = TrialOfferSignals(
            offerEligibleAtMillis = purchase.purchaseTimeMillis - TrialReminderPolicy.THIRTY_DAYS_MS - 1L,
        )
        assertFalse(TrialReminderPolicy.isFreeMonthTrial(purchase, eligibleTooLongAgo, now))
    }

    private fun state(): TrialReminderState = TrialReminderState(
        purchaseToken = "token-a",
        purchaseTimeMillis = purchaseTime,
        trialEndMillis = trialEnd,
        formattedPrice = "$2.99",
    )

    private fun observation(json: String) = TrialPurchaseObservation(
        purchaseToken = "token-a",
        purchaseTimeMillis = purchaseTime,
        originalJson = json,
    )
}
