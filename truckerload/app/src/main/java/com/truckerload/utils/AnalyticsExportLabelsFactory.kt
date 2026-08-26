package com.truckerload.utils

import android.content.Context
import com.truckerload.R
import com.truckerload.domain.model.analytics.AnalyticsFilter
import com.truckerload.domain.model.analytics.AnalyticsPeriod
import java.text.DateFormatSymbols
import java.util.Locale

fun analyticsExportLabels(
    context: Context,
    filter: AnalyticsFilter,
    ownerName: String = "",
): AnalyticsExportLabels {
    val periodLabel = analyticsPeriodLabel(context, filter)
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
        ownerLabel = context.getString(R.string.analytics_share_owner),
        ownerName = ownerName.trim(),
    )
}

fun analyticsPeriodLabel(context: Context, filter: AnalyticsFilter): String {
    val week = filter.weekNumber
    val weekYear = filter.weekYear
    if (week != null && weekYear != null) {
        return getWeekLabelShort(week, weekYear)
    }
    val month = filter.month
    val year = filter.year
    if (month != null && year != null) {
        return context.getString(
            R.string.analytics_period_month_of,
            monthLongLabel(month),
            year,
        )
    }
    if (year != null) {
        return year.toString()
    }
    return context.getString(
        when (filter.preset ?: AnalyticsPeriod.LAST_12_WEEKS) {
            AnalyticsPeriod.LAST_12_WEEKS -> R.string.analytics_period_12_weeks
            AnalyticsPeriod.LAST_6_MONTHS -> R.string.analytics_period_6_months
            AnalyticsPeriod.ALL_TIME -> R.string.analytics_period_all
        },
    )
}

private fun monthLongLabel(month: Int): String {
    val long = DateFormatSymbols(Locale.getDefault())
        .months
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
    return long.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
