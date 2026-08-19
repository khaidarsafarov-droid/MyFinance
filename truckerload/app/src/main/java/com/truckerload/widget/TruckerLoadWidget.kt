package com.truckerload.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * One UI 2×2 home-screen widget: weekly gross and goal progress.
 */
class TruckerLoadWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        WidgetRefresh.paintCached(appContext, appWidgetIds, WidgetKind.SQUARE)

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

/**
 * One UI 4×2 home-screen widget: gross, RPM, week strip, and goal.
 */
class TruckerLoadWideWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        WidgetRefresh.paintCached(appContext, appWidgetIds, WidgetKind.WIDE)

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
