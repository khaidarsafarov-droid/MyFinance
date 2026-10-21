package com.truckerload.data.premium

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.truckerload.R
import com.truckerload.domain.premium.PremiumOfferPrice
import com.truckerload.domain.premium.PremiumPolicy
import com.truckerload.domain.premium.TrialReminderPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * One notification, three days before the estimated free-month end.
 *
 * WorkManager keeps the request across process death and reboot. The boot receiver
 * only restarts Telegram and load alarms, so this job does not add another receiver.
 */
@HiltWorker
class TrialEndReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val store = TrialReminderStore(app)
        // Null means Play did not answer. Keep the reminder that was scheduled while Premium was active.
        val known = try {
            PremiumAccess.get(app).refreshKnown()
        } catch (_: Exception) {
            null
        }
        val now = System.currentTimeMillis()
        val state = store.read().state
        val premiumActive = known ?: state.purchaseToken.isNotBlank()
        val decision = TrialReminderPolicy.decide(premiumActive, state, now)
        if (!decision.postNotification) return Result.success()
        if (!canPost(app)) {
            store.clearSchedule()
            return Result.success()
        }
        postNotification(app, state.trialEndMillis, state.formattedPrice)
        store.markNotified(state.purchaseToken)
        store.clearSchedule()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "trial_end_reminder"
        private const val CHANNEL_ID = "truckerload_premium"
        private const val NOTIFY_ID = 5101

        fun canPost(context: Context): Boolean {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!manager.areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun enqueue(context: Context, triggerAtMillis: Long, nowMillis: Long) {
            val delay = (triggerAtMillis - nowMillis).coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<TrialEndReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            TrialReminderStore(context).clearSchedule()
        }

        private fun postNotification(context: Context, trialEndMillis: Long, formattedPrice: String?) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.trial_reminder_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = context.getString(R.string.trial_reminder_channel_desc)
                    },
                )
            }
            val date = TrialReminderPolicy.endDate(
                trialEndMillis,
                Locale.getDefault(),
                ZoneId.systemDefault(),
            )
            val price = PremiumOfferPrice.display(formattedPrice)
            val body = context.getString(R.string.trial_reminder_body, date, price)
            val pending = manageIntent(context)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_premium)
                .setContentTitle(context.getString(R.string.trial_reminder_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pending)
                .addAction(
                    NotificationCompat.Action.Builder(
                        null,
                        context.getString(R.string.settings_premium_manage_short),
                        pending,
                    ).build(),
                )
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(NOTIFY_ID, notification)
        }

        private fun manageIntent(context: Context): PendingIntent {
            val url = PremiumPolicy.subscriptionManagementUrl(context.packageName)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return PendingIntent.getActivity(
                context,
                NOTIFY_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
