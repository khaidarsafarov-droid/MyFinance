package com.truckerload.presentation.components

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
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
    wrapInCard: Boolean = true,
    rpmThresholds: RpmThresholds? = null,
    onCameraClick: (() -> Unit)? = null,
    onScanClick: (() -> Unit)? = null,
) {
    // Разные ветки — единственный корректный способ избежать collectAsState в каждой карточке списка.
    if (rpmThresholds != null) {
        LoadCardContent(
            load = load,
            onClick = onClick,
            modifier = modifier,
            solidBackground = solidBackground,
            wrapInCard = wrapInCard,
            rpmThresholds = rpmThresholds,
            onCameraClick = onCameraClick,
            onScanClick = onScanClick,
        )
    } else {
        val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
        LoadCardContent(
            load = load,
            onClick = onClick,
            modifier = modifier,
            solidBackground = solidBackground,
            wrapInCard = wrapInCard,
            rpmThresholds = thresholds,
            onCameraClick = onCameraClick,
            onScanClick = onScanClick,
        )
    }
}

@Composable
private fun LoadCardContent(
    load: Load,
    onClick: () -> Unit,
    modifier: Modifier,
    solidBackground: Boolean,
    wrapInCard: Boolean,
    rpmThresholds: RpmThresholds,
    onCameraClick: (() -> Unit)?,
    onScanClick: (() -> Unit)?,
) {
    val tc = LocalTruckColors.current
    val cs = MaterialTheme.colorScheme
    val route = formatLoadRoute(load)
    val stopLabel = load.stopCount.takeIf { it > 0 } ?: (load.puCount + load.delCount)
    val rpm = computeRpm(load.totalRate, load.totalMiles)
    val rpmColor = rpm?.let {
        getRpmColor(it, tc, rpmThresholds.minProfit, rpmThresholds.targetProfit)
    }

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = load.tripId,
                        style = AppTypography.CardTitle.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = cs.onSurface,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (load.isDispute) {
                        DisputeCardChip(
                            load = load,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Text(
                    text = load.effectiveFinishDate() ?: load.date,
                    style = AppTypography.CaptionMuted.copy(color = cs.onSurfaceVariant),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Text(
                text = route,
                style = AppTypography.CardRoute.copy(color = cs.onSurface),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    R.string.load_card_summary_line,
                    stopLabel,
                    String.format(Locale.US, "%,.0f", load.totalMiles),
                    String.format(Locale.US, "$%,.2f", load.totalRate),
                ),
                style = AppTypography.Caption.copy(color = cs.onSurfaceVariant),
                modifier = Modifier.padding(top = 6.dp),
            )
            if (load.durationDays > 0.0) {
                Text(
                    text = stringResource(
                        R.string.load_card_pace_line,
                        formatDurationDays(load.durationDays),
                        formatPacePerDay(load.pace),
                    ),
                    style = AppTypography.Subtitle.copy(color = cs.onSurfaceVariant),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (rpm != null && rpmColor != null) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(cs.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(rpmColor),
                        )
                        Text(
                            text = formatRpm(
                                load.totalRate,
                                load.totalMiles,
                                stringResource(R.string.rpm_per_mile_format),
                            ),
                            style = AppTypography.NumbersSmall.copy(color = cs.onSurface),
                        )
                    }
                } else {
                    Box(modifier = Modifier.size(1.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onCameraClick != null) {
                        val photoCd = stringResource(R.string.load_card_attach_photo)
                        LoadCardActionChip(
                            onClick = onCameraClick,
                            contentDescription = photoCd,
                            imageVector = AppIcons.CameraAlt,
                        )
                    }
                    if (onScanClick != null) {
                        val scanCd = stringResource(R.string.load_card_attach_scan)
                        LoadCardActionChip(
                            onClick = onScanClick,
                            contentDescription = scanCd,
                            imageVector = AppIcons.DocumentScanner,
                        )
                    }
                }
            }
        }
    }

    if (wrapInCard) {
        BentoGlassClickableCard(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = UiDimens.LoadCardMinHeight),
            solidBackground = solidBackground,
            highlight = false,
            content = { cardContent() },
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = UiDimens.LoadCardMinHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            cardContent()
        }
    }
}

/** Computes RPM. Returns null when miles are zero. */
fun computeRpm(totalRate: Double, totalMiles: Double): Double? =
    if (totalMiles > 0) totalRate / totalMiles else null

/**
 * Camera / scan chip on a load card. Fixed visual size (not Material IconButton's
 * 48dp min target) so [Arrangement.spacedBy] actually separates the green pills.
 */
@Composable
private fun LoadCardActionChip(
    onClick: () -> Unit,
    contentDescription: String,
    imageVector: ImageVector,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.primaryContainer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = cs.onPrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** RPM = Total Amount / Total Miles. */
fun formatRpm(totalRate: Double, totalMiles: Double, unitFormat: String): String {
    return if (totalMiles > 0) {
        String.format(Locale.US, unitFormat, totalRate / totalMiles)
    } else {
        "—"
    }
}

@Composable
private fun DisputeCardChip(
    load: Load,
    modifier: Modifier = Modifier,
) {
    val label = if (load.hadDispute) {
        stringResource(R.string.dispute_was_dispute)
    } else {
        stringResource(R.string.dispute_active)
    }
    val color = if (load.hadDispute) {
        LocalTruckColors.current.Success
    } else {
        LocalTruckColors.current.Danger
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = AppTypography.Caption.copy(color = color),
        )
    }
}
