package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "WidgetRefresh"
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

object WidgetRefresh {

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
            cached
        }

        val manager = AppWidgetManager.getInstance(appContext)
        val component = ComponentName(appContext, TruckerLoadWidgetReceiver::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) {
            Log.d(TAG, "No widget instances on home screen")
            return
        }

        widgetIds.forEach { id ->
            runCatching {
                val views = WidgetRemoteViews.build(appContext, id, stats)
                manager.updateAppWidget(id, views)
            }.onFailure { Log.e(TAG, "Failed to update widget $id", it) }
        }
        Log.d(TAG, "Updated ${widgetIds.size} widget(s), hasData=${stats.hasData()}")
    }

    fun refreshAndUpdateAsync(context: Context) {
        scope.launch {
            runCatching { refreshAndUpdate(context) }
                .onFailure { Log.e(TAG, "Widget refresh failed", it) }
        }
    }

    /** Paint widgets immediately from disk cache (no Room). Always paints — safe defaults when empty. */
    fun paintCached(context: Context, appWidgetIds: IntArray? = null) {
        val appContext = context.applicationContext
        val stats = WidgetDataStore.load(appContext)
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = appWidgetIds ?: manager.getAppWidgetIds(
            ComponentName(appContext, TruckerLoadWidgetReceiver::class.java)
        )
        if (ids.isEmpty()) return
        ids.forEach { id ->
            runCatching {
                val views = WidgetRemoteViews.build(appContext, id, stats)
                manager.updateAppWidget(id, views)
            }.onFailure { Log.e(TAG, "Failed to paint widget $id", it) }
        }
    }
}
