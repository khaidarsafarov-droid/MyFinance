package com.truckerload.widget.glance

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.background
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
import androidx.glance.unit.ColorProvider
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.theme.forestDarkColorScheme
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

internal val CabinGlancePrimary = Color(0xFFFFFFFF)
internal val CabinGlanceSecondary = Color(0xFF9EAEA4)
internal val CabinGlanceAccent = Color(0xFF00E676)

object OneUiGlanceWidgets {
    suspend fun updateAll(context: Context) {
        OneUiSquareGlanceWidget().updateAll(context)
        OneUiWideGlanceWidget().updateAll(context)
    }
}

internal class OneUiSquareGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(CabinSize2x2, CabinSize4x2, CabinSize4x3))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideCabinGlance(context, id)
    }
}

internal class OneUiWideGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(CabinSize4x2, CabinSize4x3))

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
    provideContent {
        CabinGlanceTheme {
            CabinBudgetBySize(
                context = context,
                shown = shown,
                week = week,
                selectedOffset = selected,
            )
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
private fun CabinGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(
        colors = ColorProviders(
            light = forestDarkColorScheme(),
            dark = forestDarkColorScheme(),
        ),
        content = content,
    )
}

@Composable
private fun SquareBudgetContent(context: Context, stats: WidgetStats) {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val ring = buildRingBitmap(context, stats, compact = true)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_cabin_plate))
            .padding(12.dp)
            .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BudgetRing(
            context = context,
            ring = ring,
            stats = stats,
            progress = progress,
            goalSet = goalSet,
            ringDp = 72.dp,
            compact = true,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_camera,
                label = context.getString(R.string.widget_camera_short),
                route = WidgetDeepLink.ROUTE_ATTACH_CAMERA,
                showLabel = false,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_scanner,
                label = context.getString(R.string.widget_scanner_short),
                route = WidgetDeepLink.ROUTE_ATTACH_SCANNER,
                showLabel = false,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            QuickAction(
                context = context,
                iconRes = R.drawable.ic_widget_diesel,
                label = context.getString(R.string.widget_diesel_short),
                launchIntent = WidgetDeepLink.dieselQuickAddIntent(context),
                showLabel = false,
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
    compact: Boolean,
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
            modifier = GlanceModifier.padding(horizontal = if (compact) 8.dp else 12.dp),
        ) {
            Text(
                text = WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
                style = TextStyle(
                    color = ColorProvider(CabinGlancePrimary),
                    fontSize = if (compact) 14.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
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
                    color = ColorProvider(CabinGlanceSecondary),
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Medium,
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
    modifier: GlanceModifier = GlanceModifier,
    route: String? = null,
    launchIntent: Intent? = null,
) {
    val intent = launchIntent ?: routeIntent(context, requireNotNull(route))
    Column(
        modifier = modifier
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .background(ImageProvider(R.drawable.widget_cabin_action_btn)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = label,
                modifier = GlanceModifier.size(18.dp),
            )
        }
        if (showLabel) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(CabinGlancePrimary),
                    fontSize = 10.sp,
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
    compact: Boolean,
): Bitmap? {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val ringPx = WidgetProgressRingBitmap.ringSizePx(
        context = context,
        compact = compact,
        expanded = !compact,
        splitLayout = !compact,
    )
    return runCatching {
        WidgetProgressRingBitmap.create(
            progressPercent = if (goalSet) progress else 0f,
            sizePx = ringPx,
            progressColor = WidgetCabinPalette.ACCENT,
            trackColor = WidgetCabinPalette.TRACK,
        )
    }.getOrNull()
}

@Composable
internal fun cabinLabelStyle(bold: Boolean = false) = TextStyle(
    color = ColorProvider(CabinGlanceSecondary),
    fontSize = 11.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
)

internal fun routeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
