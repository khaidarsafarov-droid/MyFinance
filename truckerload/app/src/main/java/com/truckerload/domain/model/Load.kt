package com.truckerload.domain.model

data class Load(
    val id: String,
    val tripId: String,
    val date: String,
    val totalRate: Double,
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
    val pace: Double = 0.0,
    val stopCount: Int = 0,
    val isDispute: Boolean = false,
    val disputeResponseDate: String? = null,
    val disputeCompleted: Boolean = false,
    /** Claimed payout for the dispute, e.g. 250.0. */
    val disputeAmount: Double? = null,
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
) {
    val isActiveDispute: Boolean get() = isDispute && !disputeCompleted
    val hadDispute: Boolean get() = isDispute && disputeCompleted
}
