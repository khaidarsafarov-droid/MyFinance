package com.truckerload.widget.glance

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
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
import com.truckerload.widget.WidgetPrefs
import com.truckerload.widget.WidgetPrefsStore
import com.truckerload.widget.WidgetTruckProgressBitmap
import com.truckerload.widget.WidgetStats
import com.truckerload.widget.WidgetStatsFormatter

internal val CabinSize2x2 = DpSize(110.dp, 110.dp)
internal val CabinSize4x2 = DpSize(250.dp, 110.dp)
internal val CabinSize4x3 = DpSize(250.dp, 180.dp)
internal val CabinSize4x4 = DpSize(250.dp, 260.dp)

private val CabinResponsiveSizes = setOf(CabinSize2x2, CabinSize4x2, CabinSize4x3, CabinSize4x4)

private val cabinSizeMode: SizeMode
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SizeMode.Exact
    } else {
        SizeMode.Responsive(CabinResponsiveSizes)
    }

internal val WidgetGlanceRevisionKey = longPreferencesKey("widget_rev")

object OneUiGlanceWidgets {
    suspend fun updateAll(context: Context) {
        val app = context.applicationContext
        bumpRevision(app, OneUiSquareGlanceWidget())
        bumpRevision(app, OneUiWideGlanceWidget())
        OneUiSquareGlanceWidget().updateAll(app)
        OneUiWideGlanceWidget().updateAll(app)
        notifyLaunchers(app)
    }

    private suspend fun bumpRevision(context: Context, widget: GlanceAppWidget) {
        val manager = GlanceAppWidgetManager(context)
        val ids = runCatching { manager.getGlanceIds(widget.javaClass) }.getOrDefault(emptyList())
        ids.forEach { glanceId ->
            runCatching {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WidgetGlanceRevisionKey] = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun notifyLaunchers(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = listOf(
            OneUiSquareGlanceReceiver::class.java,
            OneUiWideGlanceReceiver::class.java,
        ).flatMap { receiver ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).toList()
        }.toIntArray()
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                `package` = context.packageName
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            },
        )
    }
}

internal class OneUiSquareGlanceWidget : GlanceAppWidget() {
    /** Exact on API 31+ so shrinking to 2 rows switches layout; Responsive on older APIs. */
    override val sizeMode: SizeMode get() = cabinSizeMode
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideCabinGlance(context, id)
    }
}

internal class OneUiWideGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode get() = cabinSizeMode
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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
    runCatching {
        getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[WidgetGlanceRevisionKey]
    }
    val week = WidgetDataStore.load(context)
    val stored = WidgetDaySelectionStore.load(context, id)
    val todayOffset = WidgetDayProjection.todayOffset()
    val selected = WidgetDayProjection.clampSelection(stored, todayOffset)
    val shown = WidgetDayProjection.project(week, stored, todayOffset)
    val (scheme, colors) = WidgetCabinColors.resolve(context)
    val appWidgetId = runCatching {
        GlanceAppWidgetManager(context).getAppWidgetId(id)
    }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
    val prefs = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        WidgetPrefsStore.load(context, appWidgetId)
    } else {
        WidgetPrefs()
    }
    provideContent {
        CompositionLocalProvider(
            LocalCabinColors provides colors,
            LocalWidgetSizeMode provides prefs.sizeMode,
        ) {
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
    val layout = cabinLayoutFor(LocalSize.current, LocalWidgetSizeMode.current)
    val progress = stats.goalProgressPercent.coerceIn(0f, 100f)
    val goalSet = stats.weeklyProfitGoal > 0
    val colors = LocalCabinColors.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cabinPlate(colors)
            .padding(horizontal = layout.paddingH, vertical = layout.paddingV),
    ) {
        CabinHeaderSquare(context, stats.weekLabel, layout.headerSp, layout.dateSp)
        Spacer(modifier = GlanceModifier.height(layout.sectionGap))
        TruckProgressBarSquare(
            context = context,
            progress = progress,
            goalSet = goalSet,
            barDp = layout.progressBarDp,
            headroomDp = layout.progressBarHeadroomDp,
        )
        Spacer(modifier = GlanceModifier.height(layout.sectionGap))
        Row(
            modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = WidgetStatsFormatter.formatGrossUsd(stats.totalLoadRate),
                    style = TextStyle(
                        color = cabinColor(colors.text),
                        fontSize = layout.amountSp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = WidgetStatsFormatter.formatUsdRpm(stats.currentWeeklyRpm),
                    style = TextStyle(
                        color = cabinColor(colors.accent),
                        fontSize = layout.metricSp,
                    ),
                    maxLines = 1,
                )
            }
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
                Spacer(modifier = GlanceModifier.height(4.dp))
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
            }
        }
    }
}

@Composable
private fun CabinHeaderSquare(
    context: Context,
    weekLabel: String,
    headerSp: TextUnit,
    dateSp: TextUnit,
) {
    val colors = LocalCabinColors.current
    Text(
        text = context.getString(R.string.widget_brand_title_plain),
        style = TextStyle(
            color = cabinColor(colors.brand),
            fontSize = headerSp,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
        modifier = GlanceModifier.clickable(
            actionStartActivity(routeIntent(context, WidgetDeepLink.ROUTE_HOME)),
        ),
    )
}

@Composable
private fun TruckProgressBarSquare(
    context: Context,
    progress: Float,
    goalSet: Boolean,
    barDp: Dp,
    headroomDp: Dp,
) {
    val colors = LocalCabinColors.current
    val density = context.resources.displayMetrics.density
    val widthPx = (LocalSize.current.width.value * density).toInt().coerceAtLeast(80)
    val barHeightPx = (barDp.value * density).toInt().coerceAtLeast(6)
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
            modifier = GlanceModifier.fillMaxWidth().height(totalHeight),
        )
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

internal fun routeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
