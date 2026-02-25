package com.example.myfinance.data

/**
 * One load/trip from point A to B: miles, cost, cost per mile, start/end time, order number.
 * Sorted by week/month via date or weekKey.
 */
data class Trip(
    val id: String,
    val pointA: String,
    val pointB: String,
    val miles: Double,
    val cost: Double,
    val startTime: String, // ISO or readable
    val endTime: String,
    val orderNumber: String,
    val date: String, // for grouping by week/month (YYYY-MM-DD or week key)
    val companyId: String? = null
) {
    val costPerMile: Double get() = if (miles > 0) cost / miles else 0.0
}
