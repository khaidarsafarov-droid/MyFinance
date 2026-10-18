package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.PeriodFilterStyle
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import java.util.Locale

@Composable
internal fun PeriodSummarySection(
    header: HomeListItem.FilteredSectionHeader,
    currentFilter: LoadFilter,
    weeklyGoal: Double = 0.0,
    onOpenWeeklyGoal: () -> Unit = {},
) {
    val totals = header.totals
    val grossLabel = MoneyFormat.formatCurrency(totals.totalRate, decimals = 0)
    val miles = "${MoneyFormat.formatNumber(totals.totalMiles)} mi"
    val rpmValue = if (totals.totalMiles > 0) {
        String.format(Locale.US, "$%.2f", totals.avgRpm)
    } else {
        "—"
    }
    val rpmCd = stringResource(R.string.home_period_avg_rpm, rpmValue)
    val summaryCd = stringResource(
        R.string.home_period_summary_cd,
        header.label,
        grossLabel,
        miles,
        rpmCd,
    )
    HomeWeekHero(
        periodLabel = header.label,
        filter = currentFilter,
        gross = totals.totalRate,
        miles = totals.totalMiles,
        loadCount = totals.loadCount,
        avgRpm = totals.avgRpm,
        weeklyGoal = weeklyGoal,
        summaryContentDescription = summaryCd,
        onOpenWeeklyGoal = onOpenWeeklyGoal,
    )
}

@Composable
internal fun YearSectionHeader(section: YearSection) {
    val tc = LocalTruckColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = adaptiveHorizontalPadding())) {
        Text(text = stringResource(R.string.home_year_section, section.year), style = MaterialTheme.typography.titleLarge, color = tc.AccentPrimary)
        Text(
            text = stringResource(
                R.string.home_year_totals,
                section.loadCount,
                section.totalRate,
                section.totalMiles
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
internal fun MonthSectionHeader(section: MonthSection) {
    val tc = LocalTruckColors.current
    Text(
        text = stringResource(R.string.home_month_section, section.monthName, section.loads.size),
        style = MaterialTheme.typography.titleSmall,
        color = tc.TextPrimary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = adaptiveHorizontalPadding())
    )
}
