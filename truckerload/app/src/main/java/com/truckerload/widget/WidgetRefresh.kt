package com.truckerload.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "WidgetRefresh"
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val mainHandler = Handler(Looper.getMainLooper())

object WidgetRefresh {

    private val refreshMutex = Mutex()
    private var refreshRunning = false
    private var refreshQueued = false

    /**
     * Reload week stats from Room (IO), then apply Glance on Main so launchers
     * actually paint instead of deferring until the next widget tap.
     */
    suspend fun refreshAndUpdate(context: Context) {
        val appContext = context.applicationContext
        runCatching { WidgetDataProvider.refresh(appContext) }
            .onFailure { Log.e(TAG, "Stats load failed", it) }
        applyGlance(appContext)
    }

    /** Glance apply only — last committed [WidgetDataStore] snapshot. */
    suspend fun applyGlance(context: Context) {
        val appContext = context.applicationContext
        withContext(Dispatchers.Main.immediate) {
            runCatching { com.truckerload.widget.glance.OneUiGlanceWidgets.updateAll(appContext) }
                .onFailure { Log.e(TAG, "Glance widget update failed", it) }
                .onSuccess { Log.d(TAG, "Glance widgets updated") }
        }
    }

    fun refreshAndUpdateAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            val alreadyRunning = refreshMutex.withLock {
                if (refreshRunning) {
                    refreshQueued = true
                    true
                } else {
                    refreshRunning = true
                    false
                }
            }
            if (alreadyRunning) return@launch
            try {
                do {
                    runCatching { refreshAndUpdate(appContext) }
                        .onFailure { Log.e(TAG, "Widget refresh failed", it) }
                } while (
                    refreshMutex.withLock {
                        if (refreshQueued) {
                            refreshQueued = false
                            true
                        } else {
                            refreshRunning = false
                            false
                        }
                    }
                )
            } catch (t: Throwable) {
                refreshMutex.withLock {
                    refreshRunning = false
                    refreshQueued = false
                }
                throw t
            }
        }
    }

    /**
     * Paint last committed stats at the front of the main queue so the
     * launcher applies RemoteViews before [android.app.Activity.onStop].
     */
    fun paintCached(context: Context, appWidgetIds: IntArray? = null) {
        val appContext = context.applicationContext
        mainHandler.postAtFrontOfQueue {
            scope.launch(Dispatchers.Main.immediate) {
                runCatching { applyGlance(appContext) }
                    .onFailure { Log.e(TAG, "Cached Glance paint failed", it) }
            }
        }
    }

    /**
     * Leaving the app: paint cache this frame, then refresh Room and paint again.
     * OEM launchers often ignore Glance updates once the process is backgrounded
     * until the user taps the widget — this is the tap-then-it-updates bug.
     */
    fun flushForHomeScreen(context: Context) {
        val appContext = context.applicationContext
        paintCached(appContext)
        refreshAndUpdateAsync(appContext)
    }
}
