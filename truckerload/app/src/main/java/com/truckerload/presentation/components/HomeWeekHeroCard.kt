package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import java.util.Calendar
import java.util.Locale

/**
 * Home-screen hero card showing current week gross, goal progress, weekday markers, and RPM.
 */
@Composable
fun HomeWeekHeroCard(
    gross: Double,
    goal: Double,
    progressPercent: Float,
    rpm: Double?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (_, _, weekLabel) = rememberWeekLabel()
    BentoGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = RoundedCornerShape(BentoGlassTheme.CardRadius),
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .then(if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier),
        cornerRadius = BentoGlassTheme.CardRadius,
        useHeroGradient = true,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Text(
                text = weekLabel,
                style = AppTypography.CaptionMuted.copy(color = Color.White.copy(alpha = 0.88f)),
            )
            }
            AnimatedCircularProgress(
                progressPercent = progressPercent,
                gross = gross,
                goal = goal,
                modifier = Modifier.padding(vertical = 8.dp),
                size = UiDimens.HomeProgressRingSize,
                onDarkBackground = true,
            )
            WeekDayDotsRow(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
            Text(
                text = stringResource(R.string.widget_metric_cpm),
                style = AppTypography.CaptionMuted.copy(color = Color.White.copy(alpha = 0.88f)),
            )
            if (rpm != null && rpm > 0) {
                Text(
                    text = String.format(Locale.US, "$%.2f/mi", rpm),
                    style = AppTypography.HeroNumberCompact.copy(color = Color.White),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekDayDotsRow(modifier: Modifier = Modifier) {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK) // Sun=1
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val dayIndex = index + 1
            val isPast = dayIndex < today
            val isToday = dayIndex == today
            val bg = when {
                isToday -> Color.White
                isPast -> Color.White.copy(alpha = 0.85f)
                else -> Color.White.copy(alpha = 0.2f)
            }
            val labelColor = if (isToday) Color.White else Color.White.copy(alpha = 0.75f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = AppTypography.CaptionMuted.copy(color = labelColor))
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(bg),
                )
            }
        }
    }
}

@Composable
private fun rememberWeekLabel(): Triple<Int, Int, String> {
    val (week, year) = getCurrentWeekNumberAndYear()
    val (_, _, label) = getWeekRange(week, year)
    return Triple(week, year, label)
}
