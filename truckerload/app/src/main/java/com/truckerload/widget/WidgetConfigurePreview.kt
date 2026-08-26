package com.truckerload.widget

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.presentation.theme.SoftUiColors

@Composable
internal fun WidgetLivePreview(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    @Suppress("UNUSED_PARAMETER") themeMode: WidgetThemeMode,
) {
    val bg = Color(WidgetCabinPalette.BG)
    val textPrimary = Color(WidgetCabinPalette.TEXT)
    val textSecondary = Color(WidgetCabinPalette.MUTED)
    val accent = Color(WidgetCabinPalette.ACCENT)
    val track = Color(WidgetCabinPalette.RING_TRACK)
    val ringColor = Color(WidgetCabinPalette.RING)
    val compact = sizeMode == WidgetSizeMode.SMALL
    val height = when (sizeMode) {
        WidgetSizeMode.SMALL -> 100.dp
        WidgetSizeMode.MEDIUM, WidgetSizeMode.AUTO -> 148.dp
        WidgetSizeMode.LARGE -> 184.dp
    }
    val progress = if (showGoal) 0.72f else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = SoftUiColors.ShadowTint)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.widget_brand_title_plain),
                color = textPrimary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.widget_configure_preview_week),
                    color = textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }

        if (compact) {
            CompactPreviewBody(
                showGross = showGross,
                showGoal = showGoal,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
            )
        } else {
            StandardPreviewBody(
                sizeMode = sizeMode,
                showGross = showGross,
                showPace = showPace,
                showGoal = showGoal,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent,
                ringColor = ringColor,
                track = track,
                progress = progress,
            )
        }
    }
}

@Composable
private fun CompactPreviewBody(
    showGross: Boolean,
    showGoal: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (showGross) {
                Text(
                    text = stringResource(R.string.widget_configure_preview_gross),
                    color = textPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    maxLines = 1,
                )
            }
            if (showGoal) {
                Text(
                    text = "72%",
                    color = textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.widget_metric_goal),
                color = textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                text = "$13,000",
                color = textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.widget_metric_rpm),
                color = textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                text = "$1.74",
                color = accent,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        PreviewActionRow(accent = accent, labeled = false)
    }
}

@Composable
private fun StandardPreviewBody(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    ringColor: Color,
    track: Color,
    progress: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PreviewRing(
            size = if (sizeMode == WidgetSizeMode.LARGE) 78.dp else 64.dp,
            progress = progress,
            accent = ringColor,
            track = track,
            hole = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showGross) {
                        Text(
                            text = stringResource(R.string.widget_configure_preview_gross),
                            color = textPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = if (sizeMode == WidgetSizeMode.LARGE) 12.sp else 11.sp,
                            maxLines = 1,
                        )
                    }
                    if (showGoal) {
                        Text(
                            text = "72%",
                            color = textSecondary,
                            fontSize = 9.sp,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.widget_goal_out_of, "$3,450"),
                            color = textSecondary,
                            fontSize = 8.sp,
                            maxLines = 1,
                        )
                    }
                    if (!showGross && !showGoal && !showPace) {
                        Text(
                            text = stringResource(R.string.widget_configure_preview_empty),
                            color = textSecondary,
                            fontSize = 8.sp,
                            maxLines = 2,
                        )
                    }
                }
            },
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showPace) {
                Text(
                    text = stringResource(R.string.widget_configure_preview_pace),
                    color = textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
            PreviewActionRow(accent = accent, labeled = true)
        }
    }
}

@Composable
private fun PreviewRing(
    size: Dp,
    progress: Float,
    accent: Color,
    track: Color,
    hole: @Composable () -> Unit,
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = size.toPx() * (6f / 92f)
            val inset = stroke / 2f + 1.5f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        hole()
    }
}

@Composable
private fun PreviewActionRow(accent: Color, labeled: Boolean) {
    val actions = listOf(
        AppIcons.PhotoCamera to stringResource(R.string.widget_camera_short),
        AppIcons.DocumentScanner to stringResource(R.string.widget_scanner_short),
        AppIcons.LocalGasStation to stringResource(R.string.widget_diesel_short),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.forEach { (icon, label) ->
            PreviewActionChip(
                icon = icon,
                label = label,
                accent = accent,
                labeled = labeled,
            )
        }
    }
}

@Composable
private fun PreviewActionChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    labeled: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(if (labeled) 28.dp else 26.dp)
                .clip(CircleShape)
                .background(Color(WidgetCabinPalette.ACTION_BG)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(if (labeled) 15.dp else 14.dp),
            )
        }
        if (labeled) {
            Text(
                text = label,
                color = Color(WidgetCabinPalette.ACTION_LABEL),
                fontSize = 8.sp,
                maxLines = 1,
            )
        }
    }
}
