package com.truckerload.widget

import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import com.truckerload.R

object WidgetRemoteViews {

    private const val TAG = "TruckLogWidget"

    fun build(
        context: Context,
        appWidgetId: Int,
        stats: WidgetStats,
        kind: WidgetKind = WidgetKind.SQUARE,
    ): RemoteViews =
        runCatching { WidgetRemoteViewsFactory.build(context, appWidgetId, stats, kind) }
            .getOrElse { error ->
                Log.e(TAG, "Widget RemoteViews build failed for $appWidgetId", error)
                loadingFallback(context, kind)
            }

    fun build(context: Context, stats: WidgetStats): RemoteViews =
        build(context, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID, stats)

    private fun loadingFallback(context: Context, kind: WidgetKind = WidgetKind.SQUARE): RemoteViews =
        runCatching {
            WidgetRemoteViewsFactory.build(
                context.applicationContext,
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
                WidgetStats(
                    weeklyProfitGoal = com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL,
                    updatedAtMillis = System.currentTimeMillis(),
                ),
                kind,
            )
        }.getOrElse {
            RemoteViews(context.applicationContext.packageName, R.layout.widget_loading)
        }
}
