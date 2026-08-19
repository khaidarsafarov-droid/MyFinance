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
import com.truckerload.domain.crowd.CrowdRpmSnapshot
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.MoneyFormat

@Composable
internal fun CrowdRpmCard(
    snapshot: CrowdRpmSnapshot,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.crowd_rpm_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.crowd_rpm_subtitle),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
            if (snapshot.myRpm > 0.0) {
                Text(
                    text = stringResource(
                        R.string.crowd_rpm_my_week,
                        MoneyFormat.formatRpm(snapshot.myRpm),
                    ),
                    style = AppTypography.Subtitle,
                    color = tc.AccentPrimary,
                )
            } else {
                Text(
                    text = stringResource(R.string.crowd_rpm_need_loads),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
            val median = snapshot.medianRpm
            if (median != null) {
                Text(
                    text = stringResource(
                        R.string.crowd_rpm_median,
                        MoneyFormat.formatRpm(median),
                    ),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
            val percentile = snapshot.percentile
            if (percentile != null && snapshot.hasCommunity) {
                val compareRes = if (snapshot.usedSimilarLanes) {
                    R.string.crowd_rpm_percentile_similar
                } else {
                    R.string.crowd_rpm_percentile
                }
                Text(
                    text = stringResource(compareRes, percentile),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            } else {
                Text(
                    text = stringResource(R.string.crowd_rpm_waiting),
                    style = AppTypography.Subtitle,
                    color = tc.TextSecondary,
                )
            }
        }
    }
}
