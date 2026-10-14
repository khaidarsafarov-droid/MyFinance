package com.truckerload.domain.model

import com.truckerload.presentation.components.parseDisputeMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.truckerload.domain.money.Money

class DisputePayoutTest {

    @Test
    fun completeWithCheckbox_addsAmountOnce() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
        val completed = load.copy(disputeCompleted = true)
        val settled = DisputePayout.settleFrom(load, completed)
        assertEquals(Money.fromDollars(2250.0), settled.totalRate)
        assertTrue(settled.disputeAmountApplied)

        val again = DisputePayout.settleFrom(settled, settled.copy(totalRate = Money.fromDollars(2250.0)))
        assertEquals(Money.fromDollars(2250.0), again.totalRate)
    }

    @Test
    fun completeWithoutCheckbox_doesNotAdd() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = false)
        val settled = DisputePayout.settleFrom(load, load.copy(disputeCompleted = true))
        assertEquals(Money.fromDollars(2000.0), settled.totalRate)
        assertFalse(settled.disputeAmountApplied)
    }

    @Test
    fun completeWithZeroOrNullAmount_doesNotAdd() {
        val zero = sample(totalRate = 2000.0, amount = 0.0, apply = true)
        assertEquals(Money.fromDollars(2000.0), DisputePayout.settleFrom(zero, zero.copy(disputeCompleted = true)).totalRate)
        val missing = sample(totalRate = 2000.0, amount = null, apply = true)
        assertEquals(
            Money.fromDollars(2000.0),
            DisputePayout.settleFrom(missing, missing.copy(disputeCompleted = true)).totalRate,
        )
    }

    @Test
    fun uncompletePath_clearsAppliedFlag() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
            .copy(disputeCompleted = true, disputeAmountApplied = true, totalRate = Money.fromDollars(2250.0))
        val cleared = DisputePayout.settleFrom(load, load.copy(disputeCompleted = false))
        assertFalse(cleared.disputeAmountApplied)
        assertEquals(Money.fromDollars(2000.0), cleared.totalRate)
    }

    @Test
    fun uncompleteWithoutAppliedFlag_doesNotChangeRate() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
        val cleared = DisputePayout.settleFrom(load, load.copy(disputeCompleted = false))
        assertFalse(cleared.disputeAmountApplied)
        assertEquals(Money.fromDollars(2000.0), cleared.totalRate)
    }

    @Test
    fun newerUncomplete_reversesAppliedAmount() {
        val settled = sample(totalRate = 2250.0, amount = 250.0, apply = true)
            .copy(disputeCompleted = true, disputeAmountApplied = true, updatedAt = 1L)
        val incoming = settled.copy(disputeCompleted = false, updatedAt = 2L)
        val merged = DisputePayout.mergeIncoming(settled, incoming)
        val cleared = DisputePayout.settleFrom(settled, merged)
        assertFalse(cleared.disputeCompleted)
        assertFalse(cleared.disputeAmountApplied)
        assertEquals(Money.fromDollars(2000.0), cleared.totalRate)
    }

    @Test
    fun staleSnapshotAfterComplete_doesNotRollBackRate() {
        val settled = sample(totalRate = 2250.0, amount = 250.0, apply = true)
            .copy(disputeCompleted = true, disputeAmountApplied = true)
        val staleTyping = sample(totalRate = 2000.0, amount = 250.0, apply = true)
        val merged = DisputePayout.mergeIncoming(settled, staleTyping)
        val next = DisputePayout.settleFrom(settled, merged)
        assertTrue(next.disputeCompleted)
        assertTrue(next.disputeAmountApplied)
        assertEquals(Money.fromDollars(2250.0), next.totalRate)
    }

    @Test
    fun parseDisputeMoney_acceptsCommaAndBlank() {
        assertEquals(Money.fromDollars(250.0), parseDisputeMoney("250")!!)
        assertEquals(Money.fromDollars(250.5), parseDisputeMoney("250,5")!!)
        assertNull(parseDisputeMoney(""))
        assertNull(parseDisputeMoney("."))
    }

    private fun sample(
        totalRate: Double,
        amount: Double?,
        apply: Boolean,
    ) = Load(
        id = "1",
        tripId = "T-1",
        date = "2026-08-23",
        totalRate = Money.fromDollars(totalRate),
        totalMiles = 100.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 34,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        isDispute = true,
        disputeResponseDate = "2026-08-27",
        disputeCompleted = false,
        disputeAmount = amount?.let(Money::fromDollars),
        disputeApplyToLoad = apply,
    )
}
