package com.truckerload.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.PushTokenStore
import com.truckerload.data.sync.AccountCloudBackendFactory

class PushTokenRegistrationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!isFirebaseConfigured(applicationContext)) return Result.success()
        val token = PushTokenStore(applicationContext).get() ?: return Result.success()
        val auth = AuthStore(applicationContext)
        if (auth.currentUserIdOrNull() == null) return Result.success()
        if (auth.accessTokenOrNull().isNullOrBlank()) return Result.retry()
        val client = runCatching { AccountCloudBackendFactory.remoteClientOrNull(applicationContext) }
            .getOrElse {
                Log.w(TAG, "Push backend configuration is invalid")
                return Result.failure()
            }
            ?: return Result.success()
        return runCatching {
            client.registerPushToken(token)
            Result.success()
        }.getOrElse {
            Log.w(TAG, "Push token registration failed: ${it.javaClass.simpleName}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PushTokenRegistration"
        private const val UNIQUE_WORK = "push_token_registration"

        fun enqueue(context: Context) {
            if (!isFirebaseConfigured(context)) return
            val request = OneTimeWorkRequestBuilder<PushTokenRegistrationWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun isFirebaseConfigured(context: Context): Boolean =
            runCatching { FirebaseApp.getApps(context.applicationContext).isNotEmpty() }
                .getOrDefault(false)
    }
}
