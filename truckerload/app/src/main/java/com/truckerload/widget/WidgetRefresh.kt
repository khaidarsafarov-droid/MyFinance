package com.truckerload.widget

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

    suspend fun refreshAndUpdate(context: Context) {
        val appContext = context.applicationContext
        val cached = WidgetDataStore.load(appContext)
        runCatching { WidgetDataProvider.refresh(appContext) }
            .onFailure { Log.e(TAG, "Stats load failed", it) }
            .getOrNull()
            ?: cached.takeIf { it.updatedAtMillis > 0L }
            ?: WidgetStats(
                weeklyProfitGoal = com.truckerload.data.preferences.DEFAULT_WEEKLY_GROSS_GOAL,
                updatedAtMillis = System.currentTimeMillis(),
            )

        runCatching { com.truckerload.widget.glance.OneUiGlanceWidgets.updateAll(appContext) }
            .onFailure { Log.e(TAG, "Glance widget update failed", it) }
            .onSuccess { Log.d(TAG, "Glance widgets updated") }
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

    /** Glance-only: triggers async refresh from disk cache or Room. */
    fun paintCached(context: Context, appWidgetIds: IntArray? = null) {
        refreshAndUpdateAsync(context.applicationContext)
    }
}
