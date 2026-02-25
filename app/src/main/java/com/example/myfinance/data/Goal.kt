package com.example.myfinance.data

/**
 * Net profit goal for a period. When net profit (from weekly totals in period) reaches targetAmount,
 * the app notifies the user. achievedNotifiedAt is set when we've sent the "goal achieved" notification.
 */
data class Goal(
    val id: String,
    val targetAmount: Double,
    val periodStart: String, // YYYY-MM-DD
    val periodEnd: String,   // YYYY-MM-DD
    val achievedNotifiedAt: String? = null
) {
    fun markNotified(at: String) = copy(achievedNotifiedAt = at)
}
