package com.truckerload.domain.model

import com.truckerload.presentation.components.parseDisputeAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisputePayoutTest {

    @Test
    fun completeWithCheckbox_addsAmountOnce() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
        val completed = load.copy(disputeCompleted = true)
        val settled = DisputePayout.settleFrom(load, completed)
        assertEquals(2250.0, settled.totalRate, 0.0)
        assertTrue(settled.disputeAmountApplied)

        val again = DisputePayout.settleFrom(settled, settled.copy(totalRate = 2250.0))
        assertEquals(2250.0, again.totalRate, 0.0)
    }

    @Test
    fun completeWithoutCheckbox_doesNotAdd() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = false)
        val settled = DisputePayout.settleFrom(load, load.copy(disputeCompleted = true))
        assertEquals(2000.0, settled.totalRate, 0.0)
        assertFalse(settled.disputeAmountApplied)
    }

    @Test
    fun completeWithZeroOrNullAmount_doesNotAdd() {
        val zero = sample(totalRate = 2000.0, amount = 0.0, apply = true)
        assertEquals(
            2000.0,
            DisputePayout.settleFrom(zero, zero.copy(disputeCompleted = true)).totalRate,
            0.0,
        )
        val missing = sample(totalRate = 2000.0, amount = null, apply = true)
        assertEquals(
            2000.0,
            DisputePayout.settleFrom(missing, missing.copy(disputeCompleted = true)).totalRate,
            0.0,
        )
    }

    @Test
    fun uncompletePath_clearsAppliedFlag() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
            .copy(disputeCompleted = true, disputeAmountApplied = true, totalRate = 2250.0)
        val cleared = DisputePayout.settleFrom(load, load.copy(disputeCompleted = false))
        assertFalse(cleared.disputeAmountApplied)
        assertEquals(2000.0, cleared.totalRate, 0.0)
    }

    @Test
    fun uncompleteWithoutAppliedFlag_doesNotChangeRate() {
        val load = sample(totalRate = 2000.0, amount = 250.0, apply = true)
        val cleared = DisputePayout.settleFrom(load, load.copy(disputeCompleted = false))
        assertFalse(cleared.disputeAmountApplied)
        assertEquals(2000.0, cleared.totalRate, 0.0)
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
        assertEquals(2250.0, next.totalRate, 0.0)
    }

    @Test
    fun parseDisputeAmount_acceptsCommaAndBlank() {
        assertEquals(250.0, parseDisputeAmount("250")!!, 0.001)
        assertEquals(250.5, parseDisputeAmount("250,5")!!, 0.001)
        assertNull(parseDisputeAmount(""))
        assertNull(parseDisputeAmount("."))
    }

    private fun sample(
        totalRate: Double,
        amount: Double?,
        apply: Boolean,
    ) = Load(
        id = "1",
        tripId = "T-1",
        date = "2026-08-23",
        totalRate = totalRate,
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
        disputeAmount = amount,
        disputeApplyToLoad = apply,
    )
}
