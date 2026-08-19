package com.truckerload.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.data.preferences.AuthStore
import com.truckerload.di.UserComponentManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CommunityInboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
    private val userComponentManager: UserComponentManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = authStore.currentUserIdOrNull() ?: return Result.success()
        return runCatching {
            userComponentManager.startSession(userId).socialSyncCoordinator.pullRemote()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE_ONESHOT = "community_inbox_oneshot"
        const val UNIQUE_PERIODIC = "community_inbox_periodic"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CommunityInboxWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONESHOT,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<CommunityInboxWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
