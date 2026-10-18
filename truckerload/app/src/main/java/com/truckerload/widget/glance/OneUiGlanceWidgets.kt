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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.material3.ColorProviders
import com.truckerload.presentation.MainActivity
import com.truckerload.widget.WidgetCabinColors
import com.truckerload.widget.WidgetDataStore
import com.truckerload.widget.WidgetDayProjection
import com.truckerload.widget.WidgetDaySelectionStore
import com.truckerload.widget.WidgetPrefs
import com.truckerload.widget.WidgetPrefsStore
import com.truckerload.widget.WidgetStats

internal val CabinSize2x2 = DpSize(170.dp, 170.dp)
internal val CabinSize4x2 = DpSize(360.dp, 190.dp)
internal val CabinSize4x3 = DpSize(360.dp, 300.dp)
internal val CabinSize4x4 = DpSize(360.dp, 360.dp)

private val CabinResponsiveSizes = setOf(
    DpSize(110.dp, 110.dp),
    CabinSize2x2,
    DpSize(250.dp, 120.dp),
    CabinSize4x2,
    DpSize(360.dp, 236.dp),
    CabinSize4x3,
)

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
    if (size.width >= CabinWideMinWidth) {
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
    SplitCabinBody(
        context = context,
        shown = stats,
        week = stats,
        selectedOffset = WidgetDayProjection.todayOffset(),
        wide = false,
    )
}

internal fun routeIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_ROUTE, route)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
