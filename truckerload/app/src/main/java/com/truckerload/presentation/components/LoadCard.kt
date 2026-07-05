package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatDurationDays
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.domain.model.formatPacePerDay
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassClickableCard
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
fun LoadCard(
    load: Load,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solidBackground: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val rpmStore = LocalRpmThresholdsStore.current
    val thresholds by rpmStore.thresholds.collectAsState()
    val route = formatLoadRoute(load)
    val stopLabel = load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount)

    BentoGlassClickableCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.LoadCardMinHeight),
        solidBackground = solidBackground,
    ) {
        val rpm = computeRpm(load.totalRate, load.totalMiles)
        val rpmColor = rpm?.let {
            getRpmColor(it, tc, thresholds.minProfit, thresholds.targetProfit)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            if (rpmColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = UiDimens.LoadCardMinHeight)
                        .background(rpmColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (rpmColor != null) 12.dp else 0.dp),
            ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = load.tripId,
                style = AppTypography.CardTitle.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            )
            Text(
                text = load.date,
                style = AppTypography.CaptionMuted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = route,
            style = AppTypography.CardRoute,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = stringResource(
                R.string.load_card_summary_line,
                stopLabel,
                String.format(Locale.US, "%,.0f", load.totalMiles),
                String.format(Locale.US, "$%,.2f", load.totalRate),
            ),
            style = AppTypography.Caption,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (load.durationDays > 0.0) {
            Text(
                text = stringResource(
                    R.string.load_card_pace_line,
                    formatDurationDays(load.durationDays),
                    formatPacePerDay(load.pace),
                ),
                style = AppTypography.Subtitle,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (rpm != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(rpmColor!!)
                )
                Text(
                    text = formatRpm(load.totalRate, load.totalMiles, stringResource(R.string.rpm_per_mile_format)),
                    style = AppTypography.NumbersSmall,
                )
            }
        }
            }
        }
    }
}

/** Computes RPM. Returns null when miles are zero. */
fun computeRpm(totalRate: Double, totalMiles: Double): Double? =
    if (totalMiles > 0) totalRate / totalMiles else null

/** RPM = Total Amount / Total Miles. */
fun formatRpm(totalRate: Double, totalMiles: Double, unitFormat: String): String {
    return if (totalMiles > 0) {
        String.format(Locale.US, unitFormat, totalRate / totalMiles)
    } else {
        "—"
    }
}
