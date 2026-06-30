package com.truckerload.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            WidgetRefresh.refreshAndUpdate(applicationContext)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Widget update failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WORK_NAME = "truckerload_widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun refreshNow(context: Context) {
            WidgetRefresh.refreshAndUpdateAsync(context.applicationContext)
        }
    }
}
