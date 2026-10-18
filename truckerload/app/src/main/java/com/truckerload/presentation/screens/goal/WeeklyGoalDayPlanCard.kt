package com.truckerload.presentation.screens.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.goal.DayPlanRow
import com.truckerload.domain.goal.DayPlanStatus
import com.truckerload.domain.goal.WeeklyGoalProgress
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.LocalSemantics
import com.truckerload.presentation.utils.MoneyFormat
import java.time.format.DateTimeFormatter

@Composable
internal fun WeeklyGoalDayPlanCard(
    progress: WeeklyGoalProgress,
    dayPlan: List<DayPlanRow>?,
    motionSettled: Boolean,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val semantic = LocalSemantics.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(brand.card)
            .border(1.dp, brand.cardBorder, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.goal_day_plan_title),
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(semantic.warningContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.CalendarMonth,
                    contentDescription = null,
                    tint = semantic.warning,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.goal_days_left_chip, progress.daysRemainingInWeek),
                    color = semantic.warning,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (dayPlan != null) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                dayPlan.forEachIndexed { index, row ->
                    DayPlanLine(
                        row = row,
                        needed = MoneyFormat.formatCurrency(progress.dailyTargetNeeded),
                        motionSettled = motionSettled,
                        index = index,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayPlanLine(
    row: DayPlanRow,
    needed: String,
    motionSettled: Boolean,
    index: Int,
) {
    val reduce = LocalReduceMotion.current
    var shown by remember(row.date) { mutableStateOf(false) }
    LaunchedEffect(motionSettled) {
        if (motionSettled) shown = true
    }
    val locale = LocalLocale.current.platformLocale
    val dayName = row.date.format(DateTimeFormatter.ofPattern("EEEE", locale)).replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(locale) else char.toString()
    }
    val dayDate = row.date.format(DateTimeFormatter.ofPattern("d MMM", locale))
        .replace(".", "")
        .lowercase(locale)
    AnimatedVisibility(
        visible = shown,
        enter = if (reduce) {
            fadeIn(tween(0))
        } else {
            fadeIn(tween(durationMillis = 280, delayMillis = 36 * index)) +
                slideInVertically(tween(durationMillis = 280, delayMillis = 36 * index)) { it / 3 }
        },
    ) {
        DayPlanRowContent(
            row = row,
            dayName = dayName,
            dayDate = dayDate,
            needed = needed,
        )
    }
}

@Composable
private fun DayPlanRowContent(
    row: DayPlanRow,
    dayName: String,
    dayDate: String,
    needed: String,
) {
    val brand = LocalBrandPalette.current
    val today = row.status == DayPlanStatus.TODAY
    val future = row.status == DayPlanStatus.FUTURE
    val amount = if (future) needed else MoneyFormat.formatCurrency(row.gross)
    val caption = if (future) {
        stringResource(R.string.goal_day_needed)
    } else {
        stringResource(R.string.goal_day_fact, row.planPercent)
    }
    val statusCd = stringResource(
        when (row.status) {
            DayPlanStatus.TODAY -> R.string.goal_day_today_cd
            DayPlanStatus.PAST_MET -> R.string.goal_day_met_cd
            DayPlanStatus.PAST_MISSED -> R.string.goal_day_missed_cd
            DayPlanStatus.FUTURE -> R.string.goal_day_future_cd
        },
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayStatusMark(row = row, contentDescription = statusCd)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dayName,
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = if (today) stringResource(R.string.goal_day_today, dayDate) else dayDate,
                color = if (today) brand.primary else brand.textTertiary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (today) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
            Text(
                text = caption,
                color = brand.textTertiary,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DayStatusMark(row: DayPlanRow, contentDescription: String) {
    val brand = LocalBrandPalette.current
    val mark = Modifier.semantics { this.contentDescription = contentDescription }
    when (row.status) {
        DayPlanStatus.PAST_MET -> Box(
            modifier = mark
                .size(28.dp)
                .clip(CircleShape)
                .background(brand.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.Check,
                contentDescription = null,
                tint = brand.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        DayPlanStatus.TODAY -> {
            val track = brand.container
            val fill = brand.primary
            Canvas(modifier = mark.size(28.dp)) {
                val stroke = 3.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (row.planFraction > 0f) {
                    drawArc(
                        color = fill,
                        startAngle = -90f,
                        sweepAngle = 360f * row.planFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arc,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        DayPlanStatus.PAST_MISSED -> Box(
            modifier = mark.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, brand.textSecondary, CircleShape),
            )
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(2.dp)
                    .background(brand.textSecondary),
            )
        }
        DayPlanStatus.FUTURE -> Box(
            modifier = mark
                .size(28.dp)
                .border(2.dp, brand.textTertiary, CircleShape),
        )
    }
}
