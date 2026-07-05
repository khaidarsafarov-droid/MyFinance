package com.truckerload.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.WeekSummary
import com.truckerload.presentation.theme.FinanceCockpitColors
import com.truckerload.utils.getWeekLabelShort
import java.text.DateFormatSymbols
import java.util.Locale

@Composable
fun FinanceWeekStrip(
    selectedMonth: Int,
    selectedYear: Int,
    weeksInMonth: List<WeekSummary>,
    selectedWeekNumber: Int,
    selectedWeekYear: Int,
    onMonthYearClick: () -> Unit,
    onWeekSelect: (Int, Int) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthLabel = stringResource(
        R.string.finance_month_year_format,
        monthShortLabel(selectedMonth),
        selectedYear
    )
    val shimmerTransition = rememberInfiniteTransition(label = "week_shimmer")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "week_shimmer_progress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FinanceCockpitColors.GlassCard)
                    .border(1.dp, FinanceCockpitColors.GlassBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { onMonthYearClick() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = FinanceCockpitColors.SalaryAccent)
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = FinanceCockpitColors.TextPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(FinanceCockpitColors.GlassCard)
                .border(1.dp, FinanceCockpitColors.GlassBorder, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousWeek,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.finance_prev_week_cd),
                    tint = FinanceCockpitColors.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weeksInMonth.forEach { summary ->
                    val isSelected = summary.weekNumber == selectedWeekNumber && summary.year == selectedWeekYear
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) FinanceCockpitColors.ActiveDateBackground else FinanceCockpitColors.GlassCard,
                        animationSpec = tween(200)
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) FinanceCockpitColors.ActiveHighlight.copy(alpha = 0.35f) else FinanceCockpitColors.GlassBorder,
                        animationSpec = tween(200)
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) FinanceCockpitColors.ActiveHighlight else FinanceCockpitColors.InactiveDate,
                        animationSpec = tween(200)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    if (isSelected) {
                                        val shimmerX = size.width * shimmerProgress
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.26f),
                                                    Color.Transparent
                                                ),
                                                start = Offset(shimmerX - 56f, 0f),
                                                end = Offset(shimmerX + 56f, size.height)
                                            )
                                        )
                                    }
                                }
                            }
                            .clickable { onWeekSelect(summary.weekNumber, summary.year) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = getWeekLabelShort(summary.weekNumber, summary.year),
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor
                            )
                            Text(
                                text = "$${String.format(Locale.getDefault(), "%,.0f", summary.totalLoadRate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) FinanceCockpitColors.ActiveHighlight else FinanceCockpitColors.TextMuted
                            )
                        }
                    }
                }
            }
            }
            IconButton(
                onClick = onNextWeek,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.finance_next_week_cd),
                    tint = FinanceCockpitColors.TextSecondary
                )
            }
        }
    }
}

private fun monthShortLabel(month: Int): String {
    val short = DateFormatSymbols(Locale.getDefault())
        .shortMonths
        .getOrNull((month - 1).coerceIn(0, 11))
        .orEmpty()
    return short.replace(".", "")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
