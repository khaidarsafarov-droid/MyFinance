package com.truckerload.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.truckerload.R
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetSplitBitmaps
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import com.truckerload.widget.WidgetWeekDayHelper
import com.truckerload.widget.formatSplitWeekRange
import java.util.Locale

/** Glance has no res-font hook; DM Sans is the in-app face, widgets use the system sans. */
private val WidgetFace = FontFamily("sans-serif")

@Composable
internal fun SplitCabinBody(
    context: Context,
    shown: WidgetStats,
    week: WidgetStats,
    selectedOffset: Int,
    wide: Boolean,
) {
    val layout = cabinLayoutFor(LocalSize.current, LocalWidgetSizeMode.current)
    val colors = LocalCabinColors.current
    if (layout.bucket == CabinBucket.MICRO && wide) {
        MicroSplitRow(context, shown, layout, colors)
    } else {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cabinPlate(colors, layout.cornerDp),
        ) {
            SplitHeader(context, shown, layout, colors, GlanceModifier.fillMaxWidth())
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.paddingH)
                    .padding(bottom = layout.paddingV),
            ) {
                if (layout.progressBlockDp > 0.dp) {
                    Spacer(modifier = GlanceModifier.height(layout.sectionGap))
                    ProgressDetail(context, shown, layout, colors)
                }
                if (layout.showWeekRow) {
                    Spacer(modifier = GlanceModifier.height(layout.sectionGap))
                    DayBars(context, week, selectedOffset, layout, colors)
                }
                Spacer(modifier = GlanceModifier.height(layout.sectionGap))
                SplitActionRow(context, layout, colors)
            }
        }
    }
}

@Composable
private fun MicroSplitRow(
    context: Context,
    shown: WidgetStats,
    layout: CabinLayout,
    colors: WidgetCabinColors,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cabinPlate(colors, layout.cornerDp)
            .padding(horizontal = layout.paddingH, vertical = layout.paddingV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SplitHeader(
            context,
            shown,
            layout,
            colors,
            GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        SplitActionRow(context, layout, colors)
    }
}

@Composable
private fun SplitHeader(
    context: Context,
    shown: WidgetStats,
    layout: CabinLayout,
    colors: WidgetCabinColors,
    modifier: GlanceModifier,
) {
    val weekLine = headerLine(context, shown)
    val amount = WidgetStatsFormatter.formatGrossUsd(shown.totalLoadRate)
    val goal = goalMoney(context, shown)
    val percent = WidgetStatsFormatter.formatRingPercent(shown.goalProgressPercent)
    val density = context.resources.displayMetrics.density
    val headerHeight = headerBitmapHeight(layout)
    val gradient = WidgetSplitBitmaps.gradient(
        widthPx = (LocalSize.current.width.value * density).toInt().coerceAtLeast(8),
        heightPx = (headerHeight.value * density).toInt().coerceAtLeast(8),
        startColor = colors.headerStart,
        endColor = colors.headerEnd,
        topRadiusPx = layout.cornerDp.value * density,
    )
    Column(
        modifier = modifier
            .background(ImageProvider(gradient))
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME)))
            .padding(horizontal = layout.paddingH, vertical = layout.paddingV / 2),
    ) {
        if (weekLine.isNotBlank()) {
            Text(
                text = weekLine,
                style = face(colors.onHeader, layout.headerSp.value, FontWeight.Medium),
                maxLines = 1,
            )
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = amount,
                style = face(colors.onHeader, layout.amountSp.value, FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (layout.showRpm) {
                RpmPill(context, shown, colors)
            }
        }
        if (layout.bucket == CabinBucket.COMPACT) {
            Text(
                text = context.getString(R.string.widget_goal_percent, percent),
                style = face(colors.onHeader, layout.metricSp.value, FontWeight.Medium),
                maxLines = 1,
            )
        } else if (layout.showRpm) {
            Text(
                text = goal,
                style = face(colors.onHeader, layout.metricSp.value, FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (layout.headerMetric) {
            Text(
                text = context.getString(R.string.widget_goal_micro, percent, goalMoneyRaw(context, shown)),
                style = face(colors.onHeader, layout.metricSp.value, FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (layout.progressBlockDp == 0.dp && layout.progressBarDp > 0.dp) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            GoalBar(context, shown, layout.progressBarDp, layout.paddingH, colors)
        }
    }
}

@Composable
private fun RpmPill(context: Context, shown: WidgetStats, colors: WidgetCabinColors) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(50.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(colors.rpmBg)))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_STATS))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(
                R.string.widget_rpm_pill,
                WidgetStatsFormatter.formatUsdRpm(shown.currentWeeklyRpm),
            ),
            style = face(colors.rpmFg, 11f, FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun ProgressDetail(
    context: Context,
    shown: WidgetStats,
    layout: CabinLayout,
    colors: WidgetCabinColors,
) {
    val percent = WidgetStatsFormatter.formatRingPercent(shown.goalProgressPercent)
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL))),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = context.getString(R.string.widget_goal_percent, percent),
                style = face(colors.text, layout.metricSp.value, FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (layout.showRemaining) {
                Text(
                    text = context.getString(
                        R.string.widget_goal_remaining,
                        WidgetStatsFormatter.formatGrossUsd(shown.goalRemainingAmount),
                    ),
                    style = face(colors.muted, layout.metricSp.value, FontWeight.Medium),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        GoalBar(context, shown, layout.progressBarDp, layout.paddingH, colors)
    }
}

@Composable
private fun GoalBar(
    context: Context,
    shown: WidgetStats,
    barDp: Dp,
    padH: Dp,
    colors: WidgetCabinColors,
) {
    val density = context.resources.displayMetrics.density
    val contentWidth = (LocalSize.current.width.value - padH.value * 2).coerceAtLeast(40f)
    val widthPx = (contentWidth * density).toInt()
    val heightPx = (barDp.value * density).toInt().coerceAtLeast(4)
    val bitmap = WidgetSplitBitmaps.bar(
        widthPx = widthPx,
        heightPx = heightPx,
        fraction = shown.goalProgressPercent / 100f,
        fill = colors.accent,
        track = colors.actionChip,
    )
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = context.getString(R.string.widget_header_revenue) +
            ", " + context.getString(R.string.widget_weekly_summary),
        modifier = GlanceModifier.fillMaxWidth().height(barDp),
    )
}

@Composable
private fun DayBars(
    context: Context,
    week: WidgetStats,
    selectedOffset: Int,
    layout: CabinLayout,
    colors: WidgetCabinColors,
) {
    val density = context.resources.displayMetrics.density
    val weekStart = WidgetWeekDayHelper.startOfWeek()
    val labels = WidgetWeekDayHelper.localizedLabels(
        context,
        com.truckerload.domain.week.WeekStartDay.fromJavaDayOfWeek(weekStart.dayOfWeek),
    )
    val chips = WidgetWeekDayHelper.chips(week.weekLoadMask, weekStart = weekStart, labels = labels)
    val todayLabel = context.getString(R.string.widget_day_today)
    val maxGross = week.dayGross.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        chips.forEachIndexed { offset, chip ->
            val gross = week.dayGross.getOrElse(offset) { 0.0 }
            val fraction = (gross / maxGross).toFloat().coerceIn(0f, 1f)
            val barHeight = if (gross > 0.0) {
                (layout.dayBarDp * fraction).coerceAtLeast(4.dp)
            } else {
                4.dp
            }
            val cell = GlanceModifier.defaultWeight()
            Column(
                modifier = if (chip.isFuture) {
                    cell
                } else {
                    cell.clickable(
                        actionRunCallback<SelectWidgetDayAction>(
                            actionParametersOf(SelectWidgetDayAction.DAY_OFFSET to offset),
                        ),
                    )
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (layout.showDayAmounts) {
                    Text(
                        text = if (gross > 0.0) {
                            WidgetStatsFormatter.formatGrossUsd(gross)
                        } else {
                            "—"
                        },
                        style = face(colors.muted, layout.dayAmountSp.value, FontWeight.Medium),
                        maxLines = 1,
                    )
                }
                val above = layout.dayBarDp - barHeight
                if (above > 0.dp) Spacer(modifier = GlanceModifier.height(above))
                val barColor = if (chip.isToday || offset == selectedOffset) colors.dayToday else colors.dayFilled
                val barBitmap = WidgetSplitBitmaps.bar(
                    widthPx = (18 * density).toInt().coerceAtLeast(8),
                    heightPx = (barHeight.value * density).toInt().coerceAtLeast(4),
                    fraction = 1f,
                    fill = barColor,
                    track = barColor,
                    radiusPx = 4f * density,
                )
                Image(
                    provider = ImageProvider(barBitmap),
                    contentDescription = if (chip.isToday) todayLabel else chip.label,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                        .height(barHeight)
                        .cornerRadius(4.dp),
                )
                if (layout.showDayLabels) {
                    // Short weekday only. The long "today" word does not fit a 7-column cell.
                    Text(
                        text = chip.label,
                        style = TextStyle(
                            color = cabinColor(if (chip.isToday) colors.accent else colors.muted),
                            fontSize = layout.dayCaptionSp,
                            fontWeight = if (chip.isToday) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = WidgetFace,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SplitActionRow(
    context: Context,
    layout: CabinLayout,
    colors: WidgetCabinColors,
) {
    val actions = CabinQuickAction.visible(layout.actionCount)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        actions.forEachIndexed { index, action ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(8.dp))
            ActionButton(
                context = context,
                action = action,
                layout = layout,
                colors = colors,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun ActionButton(
    context: Context,
    action: CabinQuickAction,
    layout: CabinLayout,
    colors: WidgetCabinColors,
    modifier: GlanceModifier,
) {
    val label = context.getString(action.labelRes)
    val chipColor = ColorProvider(androidx.compose.ui.graphics.Color(colors.actionChip))
    if (layout.showActionLabels) {
        Row(
            modifier = modifier
                .height(layout.actionBtnDp)
                .cornerRadius(24.dp)
                .background(chipColor)
                .clickable(actionStartActivity(action.launchIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ActionIcon(action, label, layout.actionIconDp, colors)
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = label,
                style = face(colors.text, 12f, FontWeight.Medium),
                maxLines = 1,
            )
        }
    } else {
        Box(
            modifier = GlanceModifier
                .size(layout.actionBtnDp)
                .cornerRadius(24.dp)
                .background(chipColor)
                .clickable(actionStartActivity(action.launchIntent(context))),
            contentAlignment = Alignment.Center,
        ) {
            ActionIcon(action, label, layout.actionIconDp, colors)
        }
    }
}

@Composable
private fun ActionIcon(
    action: CabinQuickAction,
    label: String,
    iconDp: Dp,
    colors: WidgetCabinColors,
) {
    Image(
        provider = ImageProvider(action.iconRes),
        contentDescription = label,
        modifier = GlanceModifier.size(iconDp),
        colorFilter = ColorFilter.tint(cabinColor(colors.accent)),
    )
}

private fun headerLine(context: Context, shown: WidgetStats): String {
    val weekStart = WidgetWeekDayHelper.startOfWeek()
    val range = formatSplitWeekRange(
        weekStart,
        weekStart.plusDays(6),
        Locale.getDefault(),
    )
    return range.ifBlank { shown.weekLabel }
}

private fun headerBitmapHeight(layout: CabinLayout): Dp {
    fun line(sp: Float): Dp = if (sp <= 0f) 0.dp else (sp * 1.35f).dp + 3.dp
    val metric = layout.headerMetric || layout.showRpm || layout.bucket == CabinBucket.COMPACT
    val bar = if (layout.progressBlockDp == 0.dp && layout.progressBarDp > 0.dp) {
        layout.progressBarDp + 4.dp
    } else {
        0.dp
    }
    return layout.paddingV + line(layout.headerSp.value) + line(layout.amountSp.value) +
        (if (metric) line(layout.metricSp.value) else 0.dp) + bar
}

private fun goalMoney(context: Context, shown: WidgetStats): String =
    context.getString(R.string.widget_metric_goal_value, goalMoneyRaw(context, shown))

private fun goalMoneyRaw(context: Context, shown: WidgetStats): String =
    if (shown.weeklyProfitGoal > 0) {
        WidgetStatsFormatter.formatGrossUsd(shown.weeklyProfitGoal)
    } else {
        context.getString(R.string.widget_goal_not_set)
    }

@Composable
private fun face(argb: Int, sp: Float, weight: FontWeight): TextStyle = TextStyle(
    color = cabinColor(argb),
    fontSize = sp.sp,
    fontWeight = weight,
    fontFamily = WidgetFace,
)
