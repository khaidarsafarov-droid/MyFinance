package com.truckerload.domain.model

/**
 * When a dispute is marked completed, optionally add the claimed amount to the load rate.
 * [Load.disputeAmountApplied] prevents adding twice on later saves.
 * Un-completing subtracts that amount so [Load.totalRate] does not stay inflated.
 */
object DisputePayout {

    /**
     * Merge a UI snapshot onto the latest persisted load so a stale in-flight
     * save cannot un-complete a dispute or roll [Load.totalRate] backwards.
     * A newer snapshot that clears [Load.disputeCompleted] is the driver turning
     * the dispute back off and must reach [settleFrom].
     */
    fun mergeIncoming(previous: Load, incoming: Load): Load {
        if (previous.disputeCompleted && !incoming.disputeCompleted) {
            if (incoming.updatedAt > previous.updatedAt) {
                return incoming.copy(totalRate = previous.totalRate)
            }
            return previous.copy(
                disputeAmount = incoming.disputeAmount ?: previous.disputeAmount,
            )
        }
        if (previous.disputeCompleted) {
            return previous.copy(
                disputeAmount = incoming.disputeAmount ?: previous.disputeAmount,
            )
        }
        return incoming.copy(totalRate = previous.totalRate)
    }

    fun settleFrom(previous: Load, next: Load): Load {
        if (!next.disputeCompleted) {
            if (previous.disputeAmountApplied) {
                val amount = previous.disputeAmount ?: next.disputeAmount ?: 0.0
                val reversed = if (amount > 0.0) {
                    (previous.totalRate - amount).coerceAtLeast(0.0)
                } else {
                    next.totalRate
                }
                return next.copy(
                    totalRate = reversed,
                    disputeAmountApplied = false,
                )
            }
            return next.copy(disputeAmountApplied = false)
        }
        if (previous.disputeAmountApplied) {
            return next.copy(disputeAmountApplied = true)
        }
        val amount = next.disputeAmount ?: 0.0
        if (!next.disputeApplyToLoad || amount <= 0.0) {
            return next.copy(disputeAmountApplied = false)
        }
        return next.copy(
            totalRate = next.totalRate + amount,
            disputeAmountApplied = true,
        )
    }
}
