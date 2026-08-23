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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
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
import com.truckerload.widget.WidgetDataStore
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetProgressRingBitmap
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter
import com.truckerload.widget.WidgetWeekDayHelper
import com.truckerload.widget.WidgetWeekDaysBitmap

private val Size2x2 = DpSize(110.dp, 110.dp)
private val Size4x2 = DpSize(250.dp, 110.dp)

/** Always-dark cabin budget palette (classic home-screen ring form). */
private val CabinPrimary = Color(0xFFF2F7F4)
private val CabinSecondary = Color(0xFFC5D3C9)
private val CabinAccent = Color(0xFF5EE0A0)

object OneUiGlanceWidgets {
    suspend fun updateAll(context: Context) {
        OneUiSquareGlanceWidget().updateAll(context)
        OneUiWideGlanceWidget().updateAll(context)
    }
}

internal class OneUiSquareGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Size2x2))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stats = WidgetDataStore.load(context)
        provideContent {
            CabinGlanceTheme { SquareBudgetContent(context, stats) }
        }
    }
}

internal class OneUiWideGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Size4x2))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stats = WidgetDataStore.load(context)
        provideContent {
            CabinGlanceTheme { WideBudgetContent(context, stats) }
        }
    }
}

class OneUiSquareGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiSquareGlanceWidget()
}

class OneUiWideGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiWideGlanceWidget()
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
                route = WidgetDeepLink.ROUTE_ADD_DIESEL,
                showLabel = false,
            )
        }
    }
}

@Composable
private fun WideBudgetContent(context: Context, stats: WidgetStats) {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val rpm = stats.currentWeeklyRpm
    val ring = buildRingBitmap(context, stats, compact = false)
    val weekDays = buildWeekDaysBitmap(context, stats)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_cabin_plate))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_brand_title_plain),
                style = cabinLabelStyle(bold = true),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (stats.weekLabel.isNotBlank()) {
                Text(
                    text = stats.weekLabel,
                    style = cabinLabelStyle(),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BudgetRing(
                context = context,
                ring = ring,
                stats = stats,
                progress = progress,
                goalSet = goalSet,
                ringDp = 96.dp,
                compact = false,
                modifier = GlanceModifier.clickable(
                    actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_WEEKLY_GOAL)),
                ),
            )
            Spacer(modifier = GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (weekDays != null) {
                    Image(
                        provider = ImageProvider(weekDays),
                        contentDescription = context.getString(R.string.widget_weekly_summary),
                        modifier = GlanceModifier.fillMaxWidth().height(28.dp),
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
                Text(
                    text = context.getString(R.string.widget_rpm_label),
                    style = cabinLabelStyle(),
                    maxLines = 1,
                )
                Text(
                    text = WidgetStatsFormatter.formatRpmPerMile(context, rpm),
                    style = TextStyle(
                        color = ColorProvider(CabinAccent),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.clickable(
                        actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_STATS)),
                    ),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
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
                        route = WidgetDeepLink.ROUTE_ADD_DIESEL,
                        showLabel = true,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetRing(
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
                    color = ColorProvider(CabinPrimary),
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
            if (goalSet) {
                Text(
                    text = context.getString(
                        R.string.widget_goal_out_of,
                        WidgetStatsFormatter.formatGrossUsd(stats.weeklyProfitGoal),
                    ),
                    style = TextStyle(
                        color = ColorProvider(CabinSecondary),
                        fontSize = if (compact) 8.sp else 9.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
            Text(
                text = if (goalSet) {
                    WidgetStatsFormatter.formatRingPercent(progress)
                } else {
                    context.getString(R.string.widget_goal_not_set)
                },
                style = TextStyle(
                    color = ColorProvider(CabinSecondary),
                    fontSize = if (compact) 9.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QuickAction(
    context: Context,
    iconRes: Int,
    label: String,
    route: String,
    showLabel: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .clickable(actionStartActivity(routeIntent(context, route)))
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .background(ImageProvider(R.drawable.widget_action_btn_bg)),
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
                    color = ColorProvider(CabinSecondary),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

private fun buildRingBitmap(
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
            context,
            if (goalSet) progress else 0f,
            ringPx,
            WidgetProgressRingBitmap.progressColorForStatus(
                context,
                stats.goalPaceStatus,
                goalSet && stats.totalLoadRate >= stats.weeklyProfitGoal,
                stats.goalDaysRemaining,
            ),
        )
    }.getOrNull()
}

private fun buildWeekDaysBitmap(context: Context, stats: WidgetStats): Bitmap? {
    val chips = WidgetWeekDayHelper.chips(stats.weekLoadMask)
    val width = WidgetWeekDaysBitmap.columnWidthPx(
        context = context,
        appWidgetId = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
        horizontalPaddingDp = 14,
        ringWidthDp = 96,
        columnGapDp = 12,
    )
    val height = WidgetWeekDaysBitmap.rowHeightPx(context, compact = true)
    return runCatching {
        WidgetWeekDaysBitmap.create(context, chips, width, height)
    }.getOrNull()
}

@Composable
private fun cabinLabelStyle(bold: Boolean = false) = TextStyle(
    color = ColorProvider(CabinSecondary),
    fontSize = 11.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
)

private fun routeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
