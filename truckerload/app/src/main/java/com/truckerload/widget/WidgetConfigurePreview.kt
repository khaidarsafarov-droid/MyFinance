package com.truckerload.widget

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    val bg = if (dark) Color(0xFF1E2A24) else Color.White
    val textPrimary = if (dark) SoftUiColors.TextPrimaryDark else SoftUiColors.ForestPrimary
    val textSecondary = if (dark) SoftUiColors.TextSecondaryDark else SoftUiColors.ForestMuted
    val accent = SoftUiColors.ForestAccent
    val track = if (dark) Color(0xFF364840) else SoftUiColors.Sage
    val height = when (sizeMode) {
        WidgetSizeMode.SMALL -> 96.dp
        WidgetSizeMode.MEDIUM, WidgetSizeMode.AUTO -> 132.dp
        WidgetSizeMode.LARGE -> 168.dp
    }
    val showExtras = sizeMode != WidgetSizeMode.SMALL

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = SoftUiColors.ShadowTint)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(12.dp),
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
            Text(
                text = stringResource(R.string.widget_configure_preview_week),
                color = textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (sizeMode == WidgetSizeMode.LARGE) 56.dp else 44.dp)
                    .clip(CircleShape)
                    .background(track),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (sizeMode == WidgetSizeMode.LARGE) 40.dp else 30.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.85f)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (showGross) {
                    Text(
                        text = stringResource(R.string.widget_configure_preview_gross),
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (sizeMode == WidgetSizeMode.SMALL) 16.sp else 18.sp,
                        ),
                        maxLines = 1,
                    )
                }
                if (showPace && showExtras) {
                    Text(
                        text = stringResource(R.string.widget_configure_preview_pace),
                        color = textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
                if (showGoal && showExtras) {
                    Text(
                        text = stringResource(R.string.widget_configure_preview_goal),
                        color = accent,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                    )
                }
                if (!showGross && !showPace && !showGoal) {
                    Text(
                        text = stringResource(R.string.widget_configure_preview_empty),
                        color = textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }

        if (sizeMode == WidgetSizeMode.LARGE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index < 4) accent.copy(alpha = 0.75f) else track,
                            ),
                    )
                }
            }
        }
    }
}
