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
    /** Week trip span: first PU → last finish (respects actualFinishDate). */
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
