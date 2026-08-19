package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.crowd.CrowdRpmWeekSummary
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun CommunityCrowdRpmSection(
    summary: CrowdRpmWeekSummary,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.community_crowd_rpm_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.community_crowd_rpm_disclaimer),
                style = AppTypography.CaptionMuted,
                color = tc.TextSecondary,
            )
            if (summary.sampleCount == 0) {
                Text(
                    text = stringResource(R.string.community_crowd_rpm_empty),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.community_crowd_rpm_avg,
                        String.format(java.util.Locale.US, "%.2f", summary.avgRpm),
                    ),
                    style = AppTypography.Subtitle,
                    color = tc.TextPrimary,
                )
                Text(
                    text = stringResource(
                        R.string.community_crowd_rpm_miles,
                        MoneyFormat.formatNumber(summary.totalMiles),
                    ),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}
