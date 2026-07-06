package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

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
        WidgetRefresh.paintCached(appContext, appWidgetIds)

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
}
