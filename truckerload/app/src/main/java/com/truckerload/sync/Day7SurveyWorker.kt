package com.truckerload.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.R
import com.truckerload.data.premium.Day7SurveyStore
import com.truckerload.data.premium.PremiumAccess
import com.truckerload.domain.premium.Day7SurveyPolicy
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.navigation.Routes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * One-shot survey check. Each run re-reads Premium, opt-out, and the shared
 * display timestamp, then enqueues the next one-shot. Not a periodic worker:
 * the delay is the remaining time until the next 7-day mark.
 */
@HiltWorker
class Day7SurveyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = Day7SurveyStore(applicationContext)
        val now = System.currentTimeMillis()
        store.ensureFirstLaunch(now)
        val premium = PremiumAccess.get(applicationContext).isUnlocked()
        val permitted = Day7SurveyNotifications.canPost(applicationContext)
        val before = store.read()
        if (Day7SurveyPolicy.shouldShow(before, now, premium, ZoneId.systemDefault()) && permitted) {
            if (store.claimDisplay(now, premiumActive = premium)) {
                Day7SurveyNotifications.notify(applicationContext)
            }
        }
        val after = store.read()
        val premiumNow = PremiumAccess.get(applicationContext).isUnlocked()
        val delay = Day7SurveyPolicy.nextCheckDelayMillis(
            record = after,
            nowMillis = System.currentTimeMillis(),
            premiumActive = premiumNow,
            notificationsPermitted = Day7SurveyNotifications.canPost(applicationContext),
        )
        when {
            delay == null -> Day7SurveyScheduler.cancel(applicationContext)
            delay <= 0L -> Day7SurveyScheduler.enqueue(
                applicationContext,
                Day7SurveyPolicy.CADENCE_MILLIS,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
            else -> Day7SurveyScheduler.enqueue(
                applicationContext,
                delay,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
        }
        return Result.success()
    }
}

object Day7SurveyScheduler {
    const val UNIQUE_WORK = "day7_survey"

    fun enqueue(
        context: Context,
        delayMillis: Long,
        existing: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
    ) {
        val request = OneTimeWorkRequestBuilder<Day7SurveyWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK,
            existing,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
    }
}

object Day7SurveyNotifications {
    const val CHANNEL_ID = "truckerload_day7_survey"
    private const val NOTIFICATION_ID = 7107

    fun canPost(context: Context): Boolean {
        if (!notificationsAllowed(context)) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notify(context: Context) {
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)
        val openPaywall = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, Routes.PAYWALL)
            data = "truckerload://app/${Routes.PAYWALL}".toUri()
        }
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openPaywall,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val question = context.getString(R.string.day7_survey_question)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(question)
            .setContentText(question)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission was revoked after the check above.
        }
    }

    private fun notificationsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.day7_survey_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { setShowBadge(false) },
        )
    }
}
