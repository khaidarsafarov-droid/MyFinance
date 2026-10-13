package com.truckerload.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.truckerload.R
import com.truckerload.widget.WidgetDayCaption
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import com.truckerload.widget.WidgetTruckProgressBitmap
import com.truckerload.widget.WidgetWeekDayHelper
import com.truckerload.widget.WidgetWeekDaysBitmap

@Composable
internal fun WideBudgetContent(
    context: Context,
    shown: WidgetStats,
    week: WidgetStats,
    selectedOffset: Int,
) {
    val layout = cabinLayoutFor(LocalSize.current, LocalWidgetSizeMode.current)
    val progress = shown.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = shown.weeklyProfitGoal > 0
    val colors = LocalCabinColors.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cabinPlate(colors)
            .padding(horizontal = layout.paddingH, vertical = layout.paddingV),
    ) {
        CabinHeader(
            context,
            shown.weekLabel,
            layout.headerSp,
            layout.dateSp,
            modifier = GlanceModifier.sectionBelow(layout.sectionGap),
        )
        cabinSectionGap()
        if (layout.showGoalStatus) {
            GoalStatusRow(
                context = context,
                shown = shown,
                progress = progress,
                goalSet = goalSet,
                layout = layout,
                modifier = GlanceModifier.sectionBelow(layout.sectionGap),
            )
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .sectionBelow(layout.sectionGap)
                    .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL))),
            ) {
                TruckProgressBar(
                    context = context,
                    progress = progress,
                    goalSet = goalSet,
                    barDp = layout.progressBarDp,
                    headroomDp = layout.progressBarHeadroomDp,
                )
            }
        }
        cabinSectionGap()
        MockupStatsRow(
            context = context,
            shown = shown,
            revenueLabelSp = layout.revenueLabelSp,
            amountSp = layout.amountSp,
            metricSp = layout.metricSp,
            metricGap = layout.metricGap,
            modifier = GlanceModifier.sectionBelow(layout.sectionGap),
        )
        if (layout.showWeekRow) {
            cabinSectionGap()
            Column(modifier = GlanceModifier.fillMaxWidth().sectionBelow(layout.sectionGap)) {
                WeekDaySelector(
                    context = context,
                    week = week,
                    selectedOffset = selectedOffset,
                    chipDp = layout.dayChipDp,
                    captionSp = layout.dayCaptionSp,
                    showCaptions = layout.showDayCaptions,
                )
                if (layout.showActiveDays) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = context.getString(
                            R.string.widget_active_days,
                            WidgetStatsFormatter.formatActiveDayCount(week.totalActiveDays),
                        ),
                        style = TextStyle(
                            color = cabinColor(colors.muted),
                            fontSize = layout.dayCaptionSp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
            }
        }
        cabinSectionGap()
        CabinActionRow(context, layout, layout.showActionLabels)
    }
}

@Composable
private fun TruckProgressBar(
    context: Context,
    progress: Float,
    goalSet: Boolean,
    barDp: Dp,
    headroomDp: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val colors = LocalCabinColors.current
    val density = context.resources.displayMetrics.density
    val widthPx = (LocalSize.current.width.value * density).toInt().coerceAtLeast(120)
    val barHeightPx = (barDp.value * density).toInt().coerceAtLeast(8)
    val headroomPx = (headroomDp.value * density).toInt().coerceAtLeast(barHeightPx)
    val bitmap = runCatching {
        WidgetTruckProgressBitmap.create(
            context = context,
            progressPercent = progress,
            goalSet = goalSet,
            widthPx = widthPx,
            barHeightPx = barHeightPx,
            headroomPx = headroomPx,
            colors = colors,
        )
    }.getOrNull()
    val totalHeight = barDp + headroomDp
    if (bitmap != null) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = context.getString(R.string.widget_weekly_summary),
            modifier = modifier.fillMaxWidth().height(totalHeight),
        )
    } else {
        Spacer(modifier = modifier.height(totalHeight))
    }
}

@Composable
private fun CabinHeader(
    context: Context,
    weekLabel: String,
    headerSp: TextUnit,
    dateSp: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    val colors = LocalCabinColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_brand_title_plain),
            style = TextStyle(
                color = cabinColor(colors.brand),
                fontSize = headerSp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (weekLabel.isNotBlank()) {
            Text(
                text = weekLabel,
                style = TextStyle(
                    color = cabinColor(colors.muted),
                    fontSize = dateSp,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GoalStatusRow(
    context: Context,
    shown: WidgetStats,
    progress: Float,
    goalSet: Boolean,
    layout: CabinLayout,
    modifier: GlanceModifier = GlanceModifier,
) {
    val colors = LocalCabinColors.current
    val status = if (goalSet) {
        context.getString(paceLabel(shown.goalPaceStatus))
    } else {
        context.getString(R.string.widget_goal_not_set)
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL))),
        ) {
            Text(
                text = context.getString(R.string.widget_goal_status_line, status),
                style = TextStyle(color = cabinColor(colors.text), fontSize = layout.metricSp),
                maxLines = 2,
            )
            Spacer(modifier = GlanceModifier.height(layout.metricGap))
            Text(
                text = context.getString(
                    R.string.widget_rpm_goal_line,
                    WidgetStatsFormatter.formatUsdRpm(shown.cpmTarget),
                ),
                style = TextStyle(color = cabinColor(colors.muted), fontSize = layout.metricSp),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.width(layout.financeGap))
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL))),
        ) {
            Text(
                text = context.getString(R.string.widget_progress_label),
                style = TextStyle(color = cabinColor(colors.muted), fontSize = layout.percentSp),
                maxLines = 1,
            )
            TruckProgressBar(
                context = context,
                progress = progress,
                goalSet = goalSet,
                barDp = layout.progressBarDp,
                headroomDp = layout.progressBarHeadroomDp,
            )
        }
    }
}

private fun paceLabel(pace: String): Int = when (pace) {
    "GOAL_MET" -> R.string.widget_pace_met
    "AHEAD" -> R.string.widget_pace_ahead
    "BEHIND" -> R.string.widget_pace_behind
    else -> R.string.widget_pace_on_track
}

@Composable
private fun MockupStatsRow(
    context: Context,
    shown: WidgetStats,
    revenueLabelSp: TextUnit,
    amountSp: TextUnit,
    metricSp: TextUnit,
    metricGap: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val colors = LocalCabinColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        ) {
            Text(
                text = context.getString(R.string.widget_metric_revenue),
                style = TextStyle(
                    color = cabinColor(colors.muted),
                    fontSize = revenueLabelSp,
                ),
                maxLines = 1,
            )
            Text(
                text = WidgetStatsFormatter.formatGrossUsd(shown.totalLoadRate),
                style = TextStyle(
                    color = cabinColor(colors.text),
                    fontSize = amountSp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_STATS))),
        ) {
            Text(
                text = context.getString(R.string.widget_current_rpm),
                style = TextStyle(
                    color = cabinColor(colors.muted),
                    fontSize = metricSp,
                ),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(metricGap))
            Text(
                text = WidgetStatsFormatter.formatUsdRpm(shown.currentWeeklyRpm),
                style = TextStyle(
                    color = cabinColor(colors.accent),
                    fontSize = amountSp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WeekDaySelector(
    context: Context,
    week: WidgetStats,
    selectedOffset: Int,
    chipDp: Dp,
    captionSp: TextUnit,
    showCaptions: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val chips = WidgetWeekDayHelper.chips(week.weekLoadMask)
    val todayLabel = context.getString(R.string.widget_day_today)
    val density = context.resources.displayMetrics.density
    val chipPx = (chipDp.value * density).toInt().coerceAtLeast(24)
    val markerWidth = (chipDp.value * 0.46f).dp
    val markerHeight = 6.dp
    val todayPointer = runCatching {
        WidgetWeekDaysBitmap.createTodayPointer(
            widthPx = (markerWidth.value * density).toInt().coerceAtLeast(8),
            heightPx = (markerHeight.value * density).toInt().coerceAtLeast(6),
            color = LocalCabinColors.current.ring,
        )
    }.getOrNull()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        chips.forEachIndexed { offset, chip ->
            val bitmap = runCatching {
                WidgetWeekDaysBitmap.createChip(
                    context,
                    chip,
                    selectedOffset == offset,
                    chipPx,
                    LocalCabinColors.current,
                )
            }.getOrNull()
            val caption = WidgetDayCaption.text(
                isFuture = chip.isFuture,
                isToday = chip.isToday,
                dayGross = week.dayGross.getOrElse(offset) { 0.0 },
                todayLabel = todayLabel,
            )
            val emptyCaption = WidgetDayCaption.usesEmptyColor(
                isFuture = chip.isFuture,
                isToday = chip.isToday,
                dayGross = week.dayGross.getOrElse(offset) { 0.0 },
            )
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
                if (chip.isToday && todayPointer != null) {
                    Image(
                        provider = ImageProvider(todayPointer),
                        contentDescription = todayLabel,
                        modifier = GlanceModifier.width(markerWidth).height(markerHeight),
                    )
                } else {
                    Spacer(modifier = GlanceModifier.height(markerHeight))
                }
                Spacer(modifier = GlanceModifier.height(1.dp))
                if (bitmap != null) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = chip.label,
                        modifier = GlanceModifier.size(chipDp),
                    )
                } else {
                    Spacer(modifier = GlanceModifier.size(chipDp))
                }
                if (showCaptions) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = caption,
                        style = TextStyle(
                            color = cabinColor(
                                if (emptyCaption) {
                                    LocalCabinColors.current.dayEmptyCaption
                                } else {
                                    LocalCabinColors.current.text
                                },
                            ),
                            fontSize = captionSp,
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
internal fun CabinActionRow(context: Context, layout: CabinLayout, showLabels: Boolean = false) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CabinQuickAction.entries.forEachIndexed { index, action ->
            if (index > 0) ActionColumnDivider(height = layout.actionBtnDp)
            QuickAction(
                iconRes = action.iconRes,
                label = context.getString(action.labelRes),
                btnDp = layout.actionBtnDp,
                iconDp = layout.actionIconDp,
                showLabel = showLabels,
                labelSp = layout.dayCaptionSp,
                modifier = GlanceModifier.defaultWeight(),
                launchIntent = action.launchIntent(context),
            )
        }
    }
}

@Composable
private fun ActionColumnDivider(height: Dp) {
    Spacer(
        modifier = GlanceModifier
            .width(1.dp)
            .height(height)
            .cabinActionDivider(LocalCabinColors.current),
    )
}
