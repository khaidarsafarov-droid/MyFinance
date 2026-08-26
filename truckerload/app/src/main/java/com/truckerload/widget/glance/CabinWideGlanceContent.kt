package com.truckerload.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val tall = LocalSize.current.height >= CabinSize4x3.height
    val progress = shown.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = shown.weeklyProfitGoal > 0
    val ring = buildRingBitmap(context, shown, compact = false)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_cabin_plate))
            .padding(horizontal = 14.dp, vertical = if (tall) 10.dp else 8.dp),
    ) {
        CabinHeader(context, shown.weekLabel)
        Spacer(modifier = GlanceModifier.height(if (tall) 8.dp else 4.dp))
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
                ringDp = if (tall) 88.dp else 72.dp,
                compact = !tall,
                modifier = GlanceModifier.clickable(
                    actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL)),
                ),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            MetricStack(
                context = context,
                shown = shown,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
        }
        Spacer(modifier = GlanceModifier.height(if (tall) 8.dp else 4.dp))
        WeekDaySelector(
            context = context,
            week = week,
            selectedOffset = selectedOffset,
            showCaptions = tall,
        )
        if (tall) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(Color(0x33FFFFFF))),
            ) {}
            Spacer(modifier = GlanceModifier.height(6.dp))
        } else {
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        ActionRow(context)
    }
}

@Composable
private fun CabinHeader(context: Context, weekLabel: String) {
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (weekLabel.isNotBlank()) {
            Text(
                text = weekLabel,
                style = cabinLabelStyle(),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetricStack(
    context: Context,
    shown: WidgetStats,
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
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        MetricRow(
            label = context.getString(R.string.widget_metric_rpm),
            value = WidgetStatsFormatter.formatUsdRpm(shown.currentWeeklyRpm),
            valueColor = CabinGlanceAccent,
            onClickRoute = WidgetDeepLink.ROUTE_STATS,
            context = context,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        MetricRow(
            label = context.getString(R.string.widget_metric_trips),
            value = shown.loadsCount.toString(),
            valueColor = CabinGlancePrimary,
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color,
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
                fontSize = 11.sp,
            ),
            maxLines = 1,
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
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
    showCaptions: Boolean,
) {
    val chips = WidgetWeekDayHelper.chips(week.weekLoadMask)
    val todayLabel = context.getString(R.string.widget_day_today)
    val chipPx = WidgetWeekDaysBitmap.rowHeightPx(context, compact = true)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        chips.forEachIndexed { offset, chip ->
            val bitmap = runCatching {
                WidgetWeekDaysBitmap.createChip(context, chip, selectedOffset == offset, chipPx)
            }.getOrNull()
            val caption = WidgetDayCaption.text(
                selected = selectedOffset == offset,
                isToday = chip.isToday,
                dayGross = week.dayGross.getOrElse(offset) { 0.0 },
                todayLabel = todayLabel,
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
                        modifier = GlanceModifier.size(26.dp),
                    )
                } else {
                    Spacer(modifier = GlanceModifier.size(26.dp))
                }
                if (showCaptions) {
                    Text(
                        text = caption,
                        style = TextStyle(
                            color = ColorProvider(
                                if (selectedOffset == offset) CabinGlancePrimary else CabinGlanceSecondary,
                            ),
                            fontSize = 8.sp,
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
private fun ActionRow(context: Context) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_camera,
            label = context.getString(R.string.widget_camera_short),
            route = WidgetDeepLink.ROUTE_ATTACH_CAMERA,
            showLabel = true,
            modifier = GlanceModifier.defaultWeight(),
        )
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_scanner,
            label = context.getString(R.string.widget_scanner_short),
            route = WidgetDeepLink.ROUTE_ATTACH_SCANNER,
            showLabel = true,
            modifier = GlanceModifier.defaultWeight(),
        )
        QuickAction(
            context = context,
            iconRes = R.drawable.ic_widget_diesel,
            label = context.getString(R.string.widget_diesel_short),
            launchIntent = WidgetDeepLink.dieselQuickAddIntent(context),
            showLabel = true,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}
