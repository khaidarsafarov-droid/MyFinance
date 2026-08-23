package com.truckerload.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.PhotoCamera
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
    themeMode: WidgetThemeMode,
) {
    val dark = when (themeMode) {
        WidgetThemeMode.DARK -> true
        WidgetThemeMode.LIGHT -> false
        WidgetThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val bg = if (dark) Color(0xFF252525) else Color.White
    val textPrimary = if (dark) Color(0xFFF2F2F2) else Color(0xFF010101)
    val textSecondary = if (dark) Color(0x99F2F2F2) else Color(0x99010101)
    val accent = SoftUiColors.ForestAccent
    val track = if (dark) Color(0xFF3A3A3A) else Color(0xFFE8E8E8)
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
            .shadow(4.dp, RoundedCornerShape(22.dp), ambientColor = SoftUiColors.ShadowTint)
            .clip(RoundedCornerShape(22.dp))
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
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                track = track,
                progress = progress,
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
    track: Color,
    progress: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showGross) {
            Text(
                text = stringResource(R.string.widget_configure_preview_gross),
                color = textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(modifier = Modifier.weight(1f))
        }
        PreviewRing(
            size = 52.dp,
            progress = progress,
            accent = accent,
            track = track,
            hole = {
                if (showGoal) {
                    Text(
                        text = "72%",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            },
        )
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
            accent = accent,
            track = track,
            hole = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (showGross) {
                        Text(
                            text = stringResource(R.string.widget_configure_preview_gross),
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
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
            val stroke = size.toPx() * 0.12f
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
    val icons = listOf(
        Icons.Outlined.PhotoCamera,
        Icons.Outlined.DocumentScanner,
        Icons.Outlined.LocalGasStation,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icons.forEach { icon ->
            PreviewActionChip(icon = icon, accent = accent, labeled = labeled)
        }
    }
}

@Composable
private fun PreviewActionChip(icon: ImageVector, accent: Color, labeled: Boolean) {
    Box(
        modifier = Modifier
            .size(if (labeled) 28.dp else 26.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(if (labeled) 15.dp else 14.dp),
        )
    }
}
