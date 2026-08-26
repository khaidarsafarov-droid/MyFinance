package com.truckerload.widget.glance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetCabinPalette
import com.truckerload.widget.WidgetDataStore
import com.truckerload.widget.WidgetDayProjection
import com.truckerload.widget.WidgetDaySelectionStore
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetProgressRingBitmap
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter

internal val CabinSize2x2 = DpSize(110.dp, 110.dp)
internal val CabinSize4x2 = DpSize(250.dp, 110.dp)
internal val CabinSize4x3 = DpSize(250.dp, 180.dp)
internal val CabinSize4x4 = DpSize(250.dp, 260.dp)

object OneUiGlanceWidgets {
    suspend fun updateAll(context: Context) {
        OneUiSquareGlanceWidget().updateAll(context)
        OneUiWideGlanceWidget().updateAll(context)
    }
}

internal class OneUiSquareGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(CabinSize2x2, CabinSize4x2, CabinSize4x3, CabinSize4x4),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideCabinGlance(context, id)
    }
}

internal class OneUiWideGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(CabinSize4x2, CabinSize4x3, CabinSize4x4),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideCabinGlance(context, id)
    }
}

class OneUiSquareGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiSquareGlanceWidget()
}

class OneUiWideGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiWideGlanceWidget()
}

private suspend fun GlanceAppWidget.provideCabinGlance(context: Context, id: GlanceId) {
    val week = WidgetDataStore.load(context)
    val stored = WidgetDaySelectionStore.load(context, id)
    val todayOffset = WidgetDayProjection.todayOffset()
    val selected = WidgetDayProjection.clampSelection(stored, todayOffset)
    val shown = WidgetDayProjection.project(week, stored, todayOffset)
    val (scheme, colors) = WidgetCabinColors.resolve(context)
    provideContent {
        CompositionLocalProvider(LocalCabinColors provides colors) {
            CabinGlanceTheme(scheme) {
                CabinBudgetBySize(
                    context = context,
                    shown = shown,
                    week = week,
                    selectedOffset = selected,
                )
            }
        }
    }
}

@Composable
private fun CabinBudgetBySize(
    context: Context,
    shown: WidgetStats,
    week: WidgetStats,
    selectedOffset: Int,
) {
    val size = LocalSize.current
    if (size.width >= CabinSize4x2.width) {
        WideBudgetContent(context, shown, week, selectedOffset)
    } else {
        SquareBudgetContent(context, shown)
    }
}

@Composable
private fun CabinGlanceTheme(scheme: ColorScheme, content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(
            light = scheme,
            dark = scheme,
        ),
        content = content,
    )
}

@Composable
private fun SquareBudgetContent(context: Context, stats: WidgetStats) {
    val layout = cabinLayoutFor(LocalSize.current)
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val colors = LocalCabinColors.current
    val ring = if (layout.showRing) {
        buildRingBitmap(context, stats, layout.ringDp, colors)
    } else {
        null
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cabinPlate(colors)
            .padding(layout.paddingH)
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (layout.showRing) {
            BudgetRing(
                context = context,
                ring = ring,
                stats = stats,
                progress = progress,
                goalSet = goalSet,
                ringDp = layout.ringDp,
                amountSp = layout.amountSp,
                percentSp = layout.percentSp,
                modifier = GlanceModifier.defaultWeight(),
            )
        } else {
            CompactFinanceBlock(
                context = context,
                shown = stats,
                progress = progress,
                goalSet = goalSet,
                amountSp = layout.amountSp,
                percentSp = layout.percentSp,
                metricSp = layout.metricSp,
                metricGap = layout.metricGap,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Spacer(modifier = GlanceModifier.width(layout.financeGap))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_camera,
                label = context.getString(R.string.widget_camera_short),
                route = WidgetDeepLink.ROUTE_ATTACH_CAMERA,
                showLabel = false,
                btnDp = layout.actionBtnDp,
                iconDp = layout.actionIconDp,
                labelSp = layout.actionLabelSp,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_scanner,
                label = context.getString(R.string.widget_scanner_short),
                route = WidgetDeepLink.ROUTE_ATTACH_SCANNER,
                showLabel = false,
                btnDp = layout.actionBtnDp,
                iconDp = layout.actionIconDp,
                labelSp = layout.actionLabelSp,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_diesel,
                label = context.getString(R.string.widget_diesel_short),
                launchIntent = WidgetDeepLink.dieselQuickAddIntent(context),
                showLabel = false,
                btnDp = layout.actionBtnDp,
                iconDp = layout.actionIconDp,
                labelSp = layout.actionLabelSp,
            )
        }
    }
}

@Composable
internal fun BudgetRing(
    context: Context,
    ring: Bitmap?,
    stats: WidgetStats,
    progress: Float,
    goalSet: Boolean,
    ringDp: Dp,
    amountSp: TextUnit,
    percentSp: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier = modifier.size(ringDp),
        contentAlignment = Alignment.Center,
    ) {
        if (ring != null) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = context.getString(R.string.widget_weekly_summary),
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.padding(horizontal = 8.dp),
        ) {
            Text(
                text = WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
                style = TextStyle(
                    color = cabinColor(LocalCabinColors.current.text),
                    fontSize = amountSp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
            Text(
                text = if (goalSet) {
                    WidgetStatsFormatter.formatRingPercent(progress)
                } else {
                    context.getString(R.string.widget_goal_not_set)
                },
                style = TextStyle(
                    color = cabinColor(LocalCabinColors.current.muted),
                    fontSize = percentSp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun QuickAction(
    context: Context,
    iconRes: Int,
    label: String,
    showLabel: Boolean,
    btnDp: Dp,
    iconDp: Dp,
    labelSp: TextUnit,
    modifier: GlanceModifier = GlanceModifier,
    route: String? = null,
    launchIntent: Intent? = null,
) {
    val colors = LocalCabinColors.current
    val intent = launchIntent ?: routeIntent(context, requireNotNull(route))
    Column(
        modifier = modifier.clickable(actionStartActivity(intent)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier
                .size(btnDp)
                .cabinActionFill(colors),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = label,
                modifier = GlanceModifier.size(iconDp),
            )
        }
        if (showLabel) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = cabinColor(colors.actionLabel),
                    fontSize = labelSp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

internal fun buildRingBitmap(
    context: Context,
    stats: WidgetStats,
    ringDp: Dp,
    colors: WidgetCabinColors = WidgetCabinColors.Forest,
): Bitmap? {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val ringPx = (ringDp.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
    return runCatching {
        WidgetProgressRingBitmap.create(
            progressPercent = if (goalSet) progress else 0f,
            sizePx = ringPx,
            progressColor = colors.ring,
            trackColor = colors.ringTrack,
            strokeRatio = WidgetCabinPalette.RING_STROKE_RATIO,
        )
    }.getOrNull()
}

internal fun routeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
