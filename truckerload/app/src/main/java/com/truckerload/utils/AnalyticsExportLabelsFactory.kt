package com.truckerload.utils

import android.content.Context
import com.truckerload.R
import com.truckerload.domain.model.analytics.AnalyticsPeriod

fun analyticsExportLabels(context: Context, period: AnalyticsPeriod): AnalyticsExportLabels {
    val periodLabel = context.getString(
        when (period) {
            AnalyticsPeriod.LAST_12_WEEKS -> R.string.analytics_period_12_weeks
            AnalyticsPeriod.LAST_6_MONTHS -> R.string.analytics_period_6_months
            AnalyticsPeriod.ALL_TIME -> R.string.analytics_period_all
        },
    )
    return AnalyticsExportLabels(
        appName = BrandConstants.DISPLAY_NAME,
        title = context.getString(R.string.analytics_title),
        period = periodLabel,
        periodLine = context.getString(R.string.analytics_share_period, periodLabel),
        summarySection = context.getString(R.string.analytics_share_summary_section),
        totalLoads = context.getString(R.string.analytics_total_loads),
        totalGross = context.getString(R.string.analytics_total_gross),
        totalMiles = context.getString(R.string.analytics_total_miles),
        avgRpm = context.getString(R.string.analytics_avg_rpm),
        avgPerLoad = context.getString(R.string.analytics_avg_per_load),
        bestWeek = context.getString(R.string.analytics_best_week),
        financeSection = context.getString(R.string.analytics_finance_title),
        paycheck = context.getString(R.string.analytics_finance_paycheck),
        diesel = context.getString(R.string.analytics_finance_diesel),
        gallons = context.getString(R.string.analytics_finance_gallons),
        avgPrice = context.getString(R.string.analytics_finance_avg_price),
        savings = context.getString(R.string.analytics_share_savings),
        weeklySection = context.getString(R.string.analytics_weekly_revenue),
        weekColumn = context.getString(R.string.common_week),
        yearColumn = context.getString(R.string.common_year),
        amountColumn = context.getString(R.string.analytics_share_amount),
        milesColumn = context.getString(R.string.analytics_total_miles),
        loadsColumn = context.getString(R.string.analytics_total_loads),
        routesSection = context.getString(R.string.analytics_top_routes),
        routeColumn = context.getString(R.string.analytics_share_route),
        rpmColumn = context.getString(R.string.analytics_avg_rpm),
        dailySection = context.getString(R.string.analytics_daily_distribution),
        dayColumn = context.getString(R.string.analytics_share_day),
        empty = context.getString(R.string.analytics_chart_empty),
    )
}
