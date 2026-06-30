package com.truckerload.widget

import android.content.Context

/** Push fresh Room stats to the home-screen widget. Safe to call from ViewModel / Activity. */
object WidgetDataUpdater {
    fun updateWidgetData(context: Context) {
        WidgetRefresh.refreshAndUpdateAsync(context.applicationContext)
    }
}
