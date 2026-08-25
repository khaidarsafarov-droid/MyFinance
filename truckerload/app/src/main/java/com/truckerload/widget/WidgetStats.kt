package com.truckerload.widget

data class WidgetStats(
    val loadsCount: Int = 0,
    val avgCpm: Double = 0.0,
    val totalMiles: Double = 0.0,
    val totalLoadRate: Double = 0.0,
    val netProfit: Double = 0.0,
    val weekLabel: String = "",
    val statsLine: String = "",
    val cpmTarget: Double = 2.5,
    val weeklyProfitGoal: Double = 0.0,
    val goalProgressPercent: Float = 0f,
    val goalRemainingAmount: Double = 0.0,
    val goalDailyNeeded: Double = 0.0,
    val goalActualDailyYield: Double = 0.0,
    val goalDaysRemaining: Int = 0,
    val goalPaceStatus: String = "",
    /**
     * Bitmask of Sun–Sat days in the current trucking week that have at least one load
     * (bit 0 = Sunday). Built from [com.truckerload.domain.model.Load.date] and PU date.
     */
    val weekLoadMask: Int = 0,
    /** Per-day load counts Sun=0 … Sat=6 for the current trucking week. */
    val dayLoads: List<Int> = List(7) { 0 },
    /** Per-day gross Sun=0 … Sat=6. */
    val dayGross: List<Double> = List(7) { 0.0 },
    /** Per-day miles Sun=0 … Sat=6. */
    val dayMiles: List<Double> = List(7) { 0.0 },
    /** Sum of PU→finish active days (respects actualFinishDate). */
    val totalActiveDays: Double = 0.0,
    val updatedAtMillis: Long = 0L,
) {
    /** Weekly rate per mile: total gross ÷ total miles for the current week. */
    val currentWeeklyRpm: Double
        get() = if (totalMiles > 0) totalLoadRate / totalMiles else avgCpm

    fun hasData(): Boolean =
        updatedAtMillis > 0L && (loadsCount > 0 || totalLoadRate > 0.0 || totalMiles > 0.0 || weeklyProfitGoal > 0.0)

    fun hasWeekLoads(): Boolean = loadsCount > 0 || totalLoadRate > 0.0
}
