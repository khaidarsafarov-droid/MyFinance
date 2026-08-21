package com.truckerload.presentation.screens.assistant

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.domain.model.WeekSummary
import com.truckerload.presentation.utils.MoneyFormat

@Composable
fun weeklyGrossAnswerText(summary: WeekSummary): String {
    val rpm = if (summary.totalMiles > 0) summary.totalLoadRate / summary.totalMiles else 0.0
    return stringResource(
        R.string.assistant_weekly_gross,
        summary.weekLabel,
        MoneyFormat.formatCurrency(summary.totalLoadRate, decimals = 0),
        summary.loadsCount,
        MoneyFormat.formatNumber(summary.totalMiles, decimals = 0),
        MoneyFormat.formatRpm(rpm),
        MoneyFormat.formatCurrency(summary.dieselAmount, decimals = 0),
        MoneyFormat.formatCurrency(summary.paycheckAmount, decimals = 0),
    )
}
