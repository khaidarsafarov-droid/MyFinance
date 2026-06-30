package com.truckerload.widget

import android.content.Context

/**
 * Loads week KPIs for the home-screen widget (loads, gross, pace, goal progress, weekly RPM).
 */
object WidgetDataProvider {

    suspend fun refresh(context: Context): WidgetStats =
        WidgetStatsLoader.refresh(context.applicationContext)

    fun weeklyRpm(stats: WidgetStats): Double = stats.currentWeeklyRpm
}
