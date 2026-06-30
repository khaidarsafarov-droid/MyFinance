package com.truckerload.widget

import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.truckerload.R

object WidgetRemoteViews {

    private const val TAG = "TruckLogWidget"

    fun build(context: Context, appWidgetId: Int, stats: WidgetStats): RemoteViews =
        runCatching { WidgetRemoteViewsFactory.build(context, appWidgetId, stats) }
            .getOrElse { error ->
                Log.e(TAG, "Widget RemoteViews build failed for $appWidgetId", error)
                loadingFallback(context)
            }

    fun build(context: Context, stats: WidgetStats): RemoteViews =
        build(context, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID, stats)

    private fun loadingFallback(context: Context): RemoteViews =
        RemoteViews(context.applicationContext.packageName, R.layout.widget_loading)
}
