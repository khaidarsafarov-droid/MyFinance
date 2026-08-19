package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.FriendsLocationShareStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background presence ping. One [getCurrentLocation] then stop — no FGS.
 */
@HiltWorker
class FriendsLocationShareWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
    private val settingsDataStore: SettingsDataStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (authStore.currentUserIdOrNull().isNullOrBlank()) return Result.success()
        if (!settingsDataStore.getSharePathWithFriendsOnce()) {
            cancel(applicationContext)
            return Result.success()
        }
        if (FriendsLocationShareService.isRunning()) return Result.success()
        return runCatching {
            val publisher = FriendsLocationSharePublisher(applicationContext)
            publisher.publishOnce(FriendsLocationSharePublisher.Mode.PRESENCE_ONLY)
            Result.success()
        }.getOrElse {
            Log.w(TAG, "friends location worker failed")
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_PERIODIC = "friends_location_share_periodic"
        private const val TAG = "FriendsLocationShare"

        fun enqueuePeriodic(context: Context, intervalMinutes: Int) {
            val minutes = FriendsLocationSharePolicy.clampIntervalMinutes(intervalMinutes).toLong()
            val flex = FriendsLocationSharePolicy.workFlexMinutes(intervalMinutes)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<FriendsLocationShareWorker>(
                minutes,
                TimeUnit.MINUTES,
                flex,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(UNIQUE_PERIODIC)
        }

        suspend fun scheduledIntervalMinutes(
            settings: SettingsDataStore,
            runtime: FriendsLocationShareStore,
        ): Int = FriendsLocationSharePolicy.effectiveIntervalMinutes(
            settings.getFriendsLocationIntervalMinutesOnce(),
            runtime.lastMotion(),
        )
    }
}
