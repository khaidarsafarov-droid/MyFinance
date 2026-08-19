package com.truckerload.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.widget.WidgetDataStore
import com.truckerload.widget.WidgetDeepLink
import com.truckerload.widget.WidgetProgressRingBitmap
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter

private val Size2x2 = DpSize(110.dp, 110.dp)
private val Size4x2 = DpSize(250.dp, 110.dp)

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
        provideContent { SquareContent(context, stats) }
    }
}

internal class OneUiWideGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(Size4x2))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stats = WidgetDataStore.load(context)
        provideContent { WideContent(context, stats) }
    }
}

class OneUiSquareGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiSquareGlanceWidget()
}

class OneUiWideGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OneUiWideGlanceWidget()
}

@Composable
private fun SquareContent(context: Context, stats: WidgetStats) {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val ringPx = WidgetProgressRingBitmap.ringSizePx(
        context,
        compact = true,
        expanded = false,
    )
    val ring = runCatching {
        WidgetProgressRingBitmap.create(
            context,
            if (stats.weeklyProfitGoal > 0) progress else 0f,
            ringPx,
            WidgetProgressRingBitmap.progressColorForStatus(
                context,
                stats.goalPaceStatus,
                stats.weeklyProfitGoal > 0 && stats.totalLoadRate >= stats.weeklyProfitGoal,
            ),
        )
    }.getOrNull()

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_oneui_plate))
            .padding(16.dp)
            .clickable(actionStartActivity(homeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = context.getString(R.string.widget_brand_title_plain),
                style = labelStyle(),
                maxLines = 1,
            )
            Text(
                text = WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
                style = heroStyle(),
                maxLines = 1,
            )
            Text(
                text = if (stats.weeklyProfitGoal > 0) {
                    context.getString(R.string.widget_ring_percent_decimal, progress.toDouble())
                } else {
                    context.getString(R.string.widget_goal_not_set)
                },
                style = labelStyle(),
                maxLines = 1,
            )
        }
        if (ring != null) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = context.getString(R.string.widget_weekly_summary),
                modifier = GlanceModifier.width(56.dp).height(56.dp),
            )
        }
    }
}

@Composable
private fun WideContent(context: Context, stats: WidgetStats) {
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f) / 100f
    val rpm = if (stats.totalMiles > 0) stats.totalLoadRate / stats.totalMiles else 0.0
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_oneui_plate))
            .padding(16.dp)
            .clickable(actionStartActivity(homeIntent(context, WidgetDeepLink.ROUTE_HOME))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_brand_title_plain),
                style = labelStyle(),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            if (stats.weekLabel.isNotBlank()) {
                Text(text = stats.weekLabel, style = labelStyle(), maxLines = 1)
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
                    style = heroStyle(),
                    maxLines = 1,
                )
                Text(
                    text = if (stats.weeklyProfitGoal > 0) {
                        context.getString(
                            R.string.widget_goal_out_of,
                            WidgetStatsFormatter.formatGrossUsd(stats.weeklyProfitGoal),
                        )
                    } else {
                        context.getString(R.string.widget_goal_not_set)
                    },
                    style = labelStyle(),
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = context.getString(R.string.widget_rpm_label),
                    style = labelStyle(),
                    maxLines = 1,
                )
                Text(
                    text = WidgetStatsFormatter.formatRpmPerMile(context, rpm),
                    style = TextStyle(
                        color = ColorProvider(R.color.widget_text_primary),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(10.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
            color = ColorProvider(R.color.widget_progress_fill),
            backgroundColor = ColorProvider(R.color.widget_progress_track),
        )
    }
}

@Composable
private fun labelStyle() = TextStyle(
    color = ColorProvider(R.color.widget_text_secondary),
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
)

@Composable
private fun heroStyle() = TextStyle(
    color = ColorProvider(R.color.widget_text_primary),
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,
)

private fun homeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
