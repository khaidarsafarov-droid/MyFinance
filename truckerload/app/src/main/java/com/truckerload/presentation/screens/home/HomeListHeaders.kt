package com.truckerload.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.presentation.components.HomePeriodFilterDropdown
import com.truckerload.presentation.components.PeriodFilterStyle
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import java.util.Locale

@Composable
internal fun PeriodSummarySection(
    header: HomeListItem.FilteredSectionHeader,
    currentFilter: LoadFilter,
    selectedYear: Int?,
    selectedDateLabel: String,
    selectedWeekLabel: String,
    onFilterSelected: (LoadFilter) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    val totals = header.totals
    val gross = MoneyFormat.formatCurrency(totals.totalRate)
    val miles = "${MoneyFormat.formatNumber(totals.totalMiles)} mi"
    val rpmValue = if (totals.totalMiles > 0) {
        String.format(Locale.US, "$%.2f", totals.avgRpm)
    } else {
        "—"
    }
    val rpmLabel = stringResource(R.string.home_period_avg_rpm_label)
    val rpmCd = stringResource(R.string.home_period_avg_rpm, rpmValue)
    val summaryCd = stringResource(
        R.string.home_period_summary_cd,
        header.label,
        gross,
        miles,
        rpmCd,
    )
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onPrimaryMuted = onPrimary.copy(alpha = 0.88f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp)
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(MaterialTheme.colorScheme.primary)
            .semantics(mergeDescendants = true) { contentDescription = summaryCd },
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalShipping,
            contentDescription = null,
            tint = onPrimary.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(112.dp),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = header.label.uppercase(),
                style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
            )
            // Gross left; miles + avg RPM stacked right so the label never wraps alone.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = gross,
                    style = AppTypography.HeroNumberOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = miles,
                        style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = rpmValue,
                            style = AppTypography.HeroNumberCompact.copy(color = onPrimary),
                            maxLines = 1,
                        )
                        Text(
                            text = rpmLabel,
                            style = AppTypography.CaptionMuted.copy(color = onPrimaryMuted),
                            maxLines = 1,
                        )
                    }
                }
            }
            HomePeriodFilterDropdown(
                currentFilter = currentFilter,
                selectedYear = selectedYear,
                selectedDateLabel = selectedDateLabel,
                selectedWeekLabel = selectedWeekLabel,
                onFilterSelected = onFilterSelected,
                onOpenCalendar = onOpenCalendar,
                onOpenArchive = onOpenArchive,
                style = PeriodFilterStyle.HeroPill,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
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
