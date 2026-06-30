package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.truckerload.R

/**
 * Premium home-screen widget: loads, gross, daily pace, weekly goal progress.
 */
class TruckerLoadWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        appWidgetIds.forEach { id ->
            try {
                val stats = WidgetDataStore.load(appContext)
                val views = WidgetRemoteViews.build(appContext, id, stats)
                appWidgetManager.updateAppWidget(id, views)
            } catch (e: Exception) {
                Log.e(TAG, "Widget sync update failed for $id", e)
                runCatching {
                    appWidgetManager.updateAppWidget(
                        id,
                        RemoteViews(appContext.packageName, R.layout.widget_loading),
                    )
                }
            }
        }

        val pendingResult = goAsync()
        WidgetRefresh.refreshAndUpdateAsync(appContext)
        pendingResult.finish()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefsStore.delete(context.applicationContext, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        WidgetRefresh.refreshAndUpdateAsync(context.applicationContext)
    }

    companion object {
        private const val TAG = "TruckLogWidget"
    }
}
