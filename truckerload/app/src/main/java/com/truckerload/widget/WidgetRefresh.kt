package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "WidgetRefresh"
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

object WidgetRefresh {

    private val refreshMutex = Mutex()
    private var refreshRunning = false
    private var refreshQueued = false

    private val providers: List<Pair<Class<out android.appwidget.AppWidgetProvider>, WidgetKind>> =
        listOf(
            TruckerLoadWidgetReceiver::class.java to WidgetKind.SQUARE,
            TruckerLoadWideWidgetReceiver::class.java to WidgetKind.WIDE,
        )

    suspend fun refreshAndUpdate(context: Context) {
        val appContext = context.applicationContext
        val cached = WidgetDataStore.load(appContext)
        val fresh = runCatching { WidgetDataProvider.refresh(appContext) }
            .onFailure { Log.e(TAG, "Stats load failed", it) }
            .getOrNull()
        val stats = if (fresh != null) {
            fresh
        } else {
            Log.w(TAG, "Refresh failed — showing last cached widget data")
            cached.takeIf { it.updatedAtMillis > 0L } ?: WidgetStats(
                weeklyProfitGoal = com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL,
                updatedAtMillis = System.currentTimeMillis(),
            )
        }

        val manager = AppWidgetManager.getInstance(appContext)
        var updated = 0
        providers.forEach { (clazz, kind) ->
            val widgetIds = manager.getAppWidgetIds(ComponentName(appContext, clazz))
            widgetIds.forEach { id ->
                runCatching {
                    val views = WidgetRemoteViews.build(appContext, id, stats, kind)
                    manager.updateAppWidget(id, views)
                    updated++
                }.onFailure { Log.e(TAG, "Failed to update widget $id", it) }
            }
        }
        runCatching { com.truckerload.widget.glance.OneUiGlanceWidgets.updateAll(appContext) }
            .onFailure { Log.e(TAG, "Glance widget update failed", it) }
        if (updated == 0) {
            Log.d(TAG, "No RemoteViews widget instances on home screen")
            return
        }
        Log.d(TAG, "Updated $updated widget(s), hasData=${stats.hasData()}")
    }

    fun refreshAndUpdateAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            refreshMutex.withLock {
                if (refreshRunning) {
                    refreshQueued = true
                    return@launch
                }
                refreshRunning = true
            }
            try {
                while (true) {
                    runCatching { refreshAndUpdate(appContext) }
                        .onFailure { Log.e(TAG, "Widget refresh failed", it) }
                    val again = refreshMutex.withLock {
                        if (refreshQueued) {
                            refreshQueued = false
                            true
                        } else {
                            refreshRunning = false
                            false
                        }
                    }
                    if (!again) break
                }
            } catch (t: Throwable) {
                refreshMutex.withLock {
                    refreshRunning = false
                    refreshQueued = false
                }
                throw t
            }
        }
    }

    /** Paint widgets immediately from disk cache (no Room). Always paints — safe defaults when empty. */
    fun paintCached(
        context: Context,
        appWidgetIds: IntArray? = null,
        kind: WidgetKind = WidgetKind.SQUARE,
    ) {
        val appContext = context.applicationContext
        val stats = WidgetDataStore.load(appContext)
        val manager = AppWidgetManager.getInstance(appContext)
        val clazz = when (kind) {
            WidgetKind.SQUARE -> TruckerLoadWidgetReceiver::class.java
            WidgetKind.WIDE -> TruckerLoadWideWidgetReceiver::class.java
        }
        val ids = appWidgetIds ?: manager.getAppWidgetIds(ComponentName(appContext, clazz))
        if (ids.isEmpty()) return
        ids.forEach { id ->
            runCatching {
                val views = WidgetRemoteViews.build(appContext, id, stats, kind)
                manager.updateAppWidget(id, views)
            }.onFailure { Log.e(TAG, "Failed to paint widget $id", it) }
        }
    }
}
