package com.truckerload.presentation.screens.home

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.filter.LoadFilter
import com.truckerload.domain.money.Money
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.theme.motionDurationMs
import com.truckerload.presentation.utils.MoneyFormat
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.utils.GoalCelebrationStore
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getDaysElapsedInCurrentWeek
import com.truckerload.utils.getWeekRange
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
internal fun HomeWeekHero(
    periodLabel: String,
    filter: LoadFilter,
    gross: Money,
    miles: Double,
    loadCount: Int,
    avgRpm: Double,
    weeklyGoal: Double,
    summaryContentDescription: String,
    onOpenWeeklyGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val onHero = brand.heroText
    val muted = brand.heroTextMuted
    val isThisWeek = filter == LoadFilter.THIS_WEEK
    val (week, year) = getCurrentWeekNumberAndYear()
    val weekTitle = if (isThisWeek) {
        val range = getWeekRange(week, year).third.substringBeforeLast(",")
        stringResource(R.string.home_week_chip, week, range)
    } else {
        periodLabel
    }
    val grossText = MoneyFormat.formatCurrency(gross, decimals = 0)
    val goalMoney = Money.fromDollars(weeklyGoal)
    val showGoal = isThisWeek && goalMoney.isPositive()
    val percent = if (showGoal && goalMoney.cents > 0L) {
        (gross.cents.toFloat() / goalMoney.cents.toFloat() * 100f).coerceIn(0f, 100f)
    } else {
        0f
    }
    val rpmText = if (miles > 0) MoneyFormat.formatRpm(avgRpm) else "—"

    if (showGoal) {
        GoalReachedHaptic(percent = percent, week = week, year = year, goal = weeklyGoal)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        brand.heroStart,
                        lerp(brand.heroStart, brand.heroEnd, 0.45f),
                        brand.heroEnd,
                    ),
                ),
            )
            .semantics(mergeDescendants = true) {
                contentDescription = summaryContentDescription
            }
            .padding(20.dp),
    ) {
        Text(text = weekTitle, style = MaterialTheme.typography.bodyMedium, color = muted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grossText,
                    style = AppTypography.HeroNumberOnDark,
                    color = onHero,
                    maxLines = 1,
                )
                if (showGoal) {
                    Text(
                        text = stringResource(
                            R.string.home_week_of_goal,
                            MoneyFormat.formatCurrency(goalMoney, decimals = 0),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else if (weeklyGoal <= 0.0 && gross.isPositive()) {
                    Text(
                        text = stringResource(R.string.ux_next_set_goal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHero,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(SoftUiShapes.Chip)
                            .clickable(onClick = onOpenWeeklyGoal),
                    )
                }
            }
            if (showGoal) {
                HomeGoalRing(
                    percent = percent,
                    onHero = onHero,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(onClick = onOpenWeeklyGoal),
                )
            }
        }
        if (isThisWeek) {
            WeekDayDots(
                elapsed = getDaysElapsedInCurrentWeek(),
                onHero = onHero,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeroStat(stringResource(R.string.home_stat_rate_per_mile), rpmText, onHero, muted)
            HeroStat(
                stringResource(R.string.home_stat_miles),
                MoneyFormat.formatNumber(miles),
                onHero,
                muted,
            )
            HeroStat(
                stringResource(R.string.home_stat_loads),
                loadCount.toString(),
                onHero,
                muted,
            )
        }
    }
}

@Composable
private fun GoalReachedHaptic(percent: Float, week: Int, year: Int, goal: Double) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    LaunchedEffect(percent >= 100f, week, year, goal) {
        if (percent < 100f) return@LaunchedEffect
        if (GoalCelebrationStore.wasRingHapticSent(context, week, year, goal)) return@LaunchedEffect
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        GoalCelebrationStore.markRingHaptic(context, week, year, goal)
    }
}

@Composable
private fun HomeGoalRing(
    percent: Float,
    onHero: Color,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val animated by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = tween(
            durationMillis = motionDurationMs(reduceMotion, 900),
            easing = EaseOutCubic,
        ),
        label = "homeGoalRing",
    )
    val caption = stringResource(R.string.home_goal_of_percent, 0)
        .replace("0%", "")
        .trim()
    Box(modifier = modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(92.dp)) {
            val stroke = 8.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arc = Size(diameter, diameter)
            drawArc(
                color = onHero.copy(alpha = 0.28f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arc,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (animated > 0f) {
                drawArc(
                    color = onHero,
                    startAngle = -90f,
                    sweepAngle = 360f * (animated / 100f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animated.toInt()}%",
                style = AppTypography.NumbersSmall.copy(color = onHero, fontWeight = FontWeight.Bold),
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = onHero.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun WeekDayDots(elapsed: Int, onHero: Color, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val (week, year) = getCurrentWeekNumberAndYear()
    val start = LocalDate.parse(getWeekRange(week, year).first)
    val labels = (0 until 7).map { offset ->
        start.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { index, label ->
            val filled = index < elapsed.coerceIn(0, 7)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = onHero.copy(alpha = if (filled) 1f else 0.55f),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(onHero.copy(alpha = if (filled) 1f else 0.28f)),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, onHero: Color, muted: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = muted)
        Text(
            text = value,
            style = AppTypography.NumbersSmall.copy(color = onHero, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
