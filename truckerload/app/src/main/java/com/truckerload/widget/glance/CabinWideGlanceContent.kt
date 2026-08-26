package com.truckerload.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import androidx.glance.unit.ColorProvider
import com.truckerload.R
import com.truckerload.widget.WidgetDayCaption
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import com.truckerload.widget.WidgetWeekDayHelper
import com.truckerload.widget.WidgetWeekDaysBitmap

@Composable
internal fun WideBudgetContent(
    context: Context,
    shown: WidgetStats,
    week: WidgetStats,
    selectedOffset: Int,
) {
    val layout = cabinLayoutFor(LocalSize.current)
    val progress = shown.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = shown.weeklyProfitGoal > 0
    val ring = buildRingBitmap(context, shown, layout.ringDp)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_cabin_plate))
            .padding(horizontal = layout.paddingH, vertical = layout.paddingV),
    ) {
        CabinHeader(context, shown.weekLabel, layout.headerSp, layout.dateSp)
        Spacer(modifier = GlanceModifier.height(layout.sectionGap))
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BudgetRing(
                context = context,
                ring = ring,
                stats = shown,
                progress = progress,
                goalSet = goalSet,
                ringDp = layout.ringDp,
                amountSp = layout.amountSp,
                percentSp = layout.percentSp,
                modifier = GlanceModifier.clickable(
                    actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL)),
                ),
            )
            Spacer(modifier = GlanceModifier.width(layout.financeGap))
            MetricStack(
                context = context,
                shown = shown,
                metricSp = layout.metricSp,
                metricGap = layout.metricGap,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
        }
        Spacer(modifier = GlanceModifier.height(layout.sectionGap))
        WeekDaySelector(
            context = context,
            week = week,
            selectedOffset = selectedOffset,
            chipDp = layout.dayChipDp,
            captionSp = layout.dayCaptionSp,
            showCaptions = layout.showDayCaptions,
        )
        if (layout.showDivider) {
            Spacer(modifier = GlanceModifier.height(layout.sectionGap))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(CabinGlanceDivider)),
            ) {}
            Spacer(modifier = GlanceModifier.height(14.dp))
        } else {
            Spacer(modifier = GlanceModifier.height(layout.sectionGap))
        }
        ActionRow(context, layout)
    }
}

@Composable
private fun CabinHeader(
    context: Context,
    weekLabel: String,
    headerSp: TextUnit,
    dateSp: TextUnit,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_brand_title_plain),
            style = TextStyle(
                color = ColorProvider(CabinGlancePrimary),
                fontSize = headerSp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (weekLabel.isNotBlank()) {
            Text(
                text = weekLabel,
                style = TextStyle(
                    color = ColorProvider(CabinGlanceSecondary),
                    fontSize = dateSp,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetricStack(
    context: Context,
    shown: WidgetStats,
    metricSp: TextUnit,
    metricGap: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricRow(
            label = context.getString(R.string.widget_metric_goal),
            value = if (shown.weeklyProfitGoal > 0) {
                WidgetStatsFormatter.formatGrossUsd(shown.weeklyProfitGoal)
            } else {
                context.getString(R.string.widget_goal_not_set)
            },
            valueColor = CabinGlancePrimary,
            fontSize = metricSp,
        )
        Spacer(modifier = GlanceModifier.height(metricGap))
        MetricRow(
            label = context.getString(R.string.widget_metric_rpm),
            value = WidgetStatsFormatter.formatUsdRpm(shown.currentWeeklyRpm),
            valueColor = CabinGlanceAccent,
            fontSize = metricSp,
            onClickRoute = WidgetDeepLink.ROUTE_STATS,
            context = context,
        )
        Spacer(modifier = GlanceModifier.height(metricGap))
        MetricRow(
            label = context.getString(R.string.widget_metric_trips),
            value = shown.loadsCount.toString(),
            valueColor = CabinGlancePrimary,
            fontSize = metricSp,
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color,
    fontSize: TextUnit,
    context: Context? = null,
    onClickRoute: String? = null,
) {
    val style = GlanceModifier.fillMaxWidth().let { base ->
        if (onClickRoute != null && context != null) {
            base.clickable(actionStartActivity(routeIntent(context, onClickRoute)))
        } else {
            base
        }
    }
    Row(
        modifier = style,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(CabinGlanceSecondary),
                fontSize = fontSize,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
        )
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
) {
    val chips = WidgetWeekDayHelper.chips(week.weekLoadMask)
    val todayLabel = context.getString(R.string.widget_day_today)
    val chipPx = (chipDp.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        chips.forEachIndexed { offset, chip ->
            val bitmap = runCatching {
                WidgetWeekDaysBitmap.createChip(context, chip, selectedOffset == offset, chipPx)
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
                            color = ColorProvider(
                                if (emptyCaption) CabinGlanceEmptyCaption else CabinGlancePrimary,
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
private fun ActionRow(context: Context, layout: CabinLayout) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_camera,
            label = context.getString(R.string.widget_camera_short),
            route = WidgetDeepLink.ROUTE_ATTACH_CAMERA,
            showLabel = layout.showActionLabels,
            btnDp = layout.actionBtnDp,
            iconDp = layout.actionIconDp,
            labelSp = layout.actionLabelSp,
            modifier = GlanceModifier.defaultWeight(),
        )
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_scanner,
            label = context.getString(R.string.widget_scanner_short),
            route = WidgetDeepLink.ROUTE_ATTACH_SCANNER,
            showLabel = layout.showActionLabels,
            btnDp = layout.actionBtnDp,
            iconDp = layout.actionIconDp,
            labelSp = layout.actionLabelSp,
            modifier = GlanceModifier.defaultWeight(),
        )
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_diesel,
            label = context.getString(R.string.widget_diesel_short),
            launchIntent = WidgetDeepLink.dieselQuickAddIntent(context),
            showLabel = layout.showActionLabels,
            btnDp = layout.actionBtnDp,
            iconDp = layout.actionIconDp,
            labelSp = layout.actionLabelSp,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}
