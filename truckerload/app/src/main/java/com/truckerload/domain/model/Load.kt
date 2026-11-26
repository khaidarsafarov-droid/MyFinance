package com.truckerload.domain.model

import com.truckerload.domain.money.Money

data class Load(
    val id: String,
    val tripId: String,
    val date: String,
    val totalRate: Money,
    val totalMiles: Double,
    val pointA: String,
    val pointB: String,
    val puCount: Int,
    val delCount: Int,
    val weekNumber: Int,
    val year: Int,
    val rawMessage: String,
    val parsedAt: Long,
    val updatedAt: Long,
    val route: String = "",
    val firstPuCityState: String = "",
    val lastDelCityState: String = "",
    val durationDays: Double = 0.0,
    val pace: Money = Money.ZERO,
    val stopCount: Int = 0,
    val isDispute: Boolean = false,
    val disputeResponseDate: String? = null,
    val disputeCompleted: Boolean = false,
    /** Claimed payout for the dispute, e.g. 250.0. */
    val disputeAmount: Money? = null,
    /** If true, [disputeAmount] is added to [totalRate] when the dispute is completed. */
    val disputeApplyToLoad: Boolean = false,
    /** True after [disputeAmount] has already been folded into [totalRate]. */
    val disputeAmountApplied: Boolean = false,
    /**
     * Фактическое окончание груза, если водитель закончил раньше/иначе, чем по последнему DEL.
     * `"YYYY-MM-DD"` (legacy, конец дня) или `"YYYY-MM-DD HH:mm"`. null = из стопов.
     */
    val actualFinishDate: String? = null,
    val stops: List<Stop> = emptyList(),
    val penalties: List<Penalty> = emptyList(),
    val equipmentType: EquipmentType? = null,
    /** Empty miles driven to pick up this load. Added to [drivenMiles]. */
    val deadheadMiles: Double = 0.0,
    /**
     * Broker or shipper cancelled the load after it was booked. While true, [totalRate]
     * holds the TONU (truck ordered, not used) pay and [totalMiles] is 0. See [LoadCancellation].
     */
    val isCancelled: Boolean = false,
    /** When the driver marked the load cancelled (epoch ms); null when not cancelled. */
    val cancelledAt: Long? = null,
    /** Rate before the cancellation, restored by undo. Null when not cancelled. */
    val originalRate: Money? = null,
    /** Loaded miles before the cancellation, restored by undo. Null when not cancelled. */
    val originalMiles: Double? = null,
) {
    val isActiveDispute: Boolean get() = isDispute && !disputeCompleted
    val hadDispute: Boolean get() = isDispute && disputeCompleted

    /** TONU pay for a cancelled load; null when the load is not cancelled. */
    val tonuAmount: Money? get() = if (isCancelled) totalRate else null

    /** Loaded miles plus deadhead. This is the load's mileage in the journal. */
    val drivenMiles: Double
        get() = totalMiles + deadheadMiles.coerceAtLeast(0.0)
}
