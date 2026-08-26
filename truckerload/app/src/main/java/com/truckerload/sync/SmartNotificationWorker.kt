package com.truckerload.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.truckerload.R
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.di.UserComponentManager
import com.truckerload.domain.week.WeekStartRuntime
import com.truckerload.domain.notifications.QuietHours
import com.truckerload.utils.getCurrentWeekNumberAndYear
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.truckerload.utils.shiftWeekNumberAndYear
import java.util.Calendar
import kotlinx.coroutines.flow.first

/**
 * At most one paycheck + one diesel reminder per ISO week, and one bundled maintenance alert
 * (not one shade entry per task). Stable notification IDs prevent stacking duplicates.
 */
@HiltWorker
class SmartNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val authStore: AuthStore,
    private val userComponentManager: UserComponentManager,
    private val settingsDataStore: SettingsDataStore,
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_MISSING = "truckerload_missing"
        const val CHANNEL_ALERTS = "truckerload_alerts"
        private const val PREFS = "smart_notifications"
        private const val KEY_MISSING_WEEK = "missing_notified_week"
        private const val ID_PAYCHECK = 1
        private const val ID_DIESEL = 2
        private const val ID_MAINTENANCE_BUNDLE = 100
        private const val LEGACY_MAINTENANCE_ID_START = 100
        private const val LEGACY_MAINTENANCE_ID_END = 130
    }

    override suspend fun doWork(): Result {
        val userId = authStore.currentUserIdOrNull() ?: return Result.success()
        val session = userComponentManager.startSession(userId)
        val paycheckRepo = session.paycheckRepository
        val dieselRepo = session.dieselRepository
        val maintenanceRepo = session.maintenanceRepository

        val quietEnabled = settingsDataStore.getQuietHoursEnabledOnce()
        val quietStart = settingsDataStore.getQuietHoursStartOnce()
        val quietEnd = settingsDataStore.getQuietHoursEndOnce()
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (QuietHours.isActive(nowHour, quietStart, quietEnd, quietEnabled)) {
            return Result.success()
        }

        val allowMissingWeek = settingsDataStore.getNotifyMissingWeekOnce()
        val allowMaintenance = settingsDataStore.getNotifyMaintenanceOnce()

        createChannels()
        cancelLegacyPerTaskMaintenanceAlerts()
        val (currentWeek, year) = getCurrentWeekNumberAndYear()
        val (lastWeek, lastYear) = shiftWeekNumberAndYear(currentWeek, year, -1)
        val (dieselCurrentWeek, dieselCurrentYear) = getCurrentWeekNumberAndYear(WeekStartRuntime.diesel)
        val (lastDieselWeek, lastDieselYear) = shiftWeekNumberAndYear(
            dieselCurrentWeek,
            dieselCurrentYear,
            -1,
            WeekStartRuntime.diesel,
        )
        val weekKey = "$lastYear-W$lastWeek"
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alreadyNotifiedMissing = prefs.getString(KEY_MISSING_WEEK, null) == weekKey

        return try {
            val paycheck = paycheckRepo.getPaycheckForWeek(lastWeek, lastYear)
            val diesel = dieselRepo.getDieselForWeek(lastDieselWeek, lastDieselYear).first()
            val dueMaintenance = if (allowMaintenance) {
                maintenanceRepo.getDueProgressForNotifications()
            } else {
                emptyList()
            }
            val plan = SmartNotificationPlanner.plan(
                hasPaycheckForLastWeek = paycheck != null,
                dieselEntriesLastWeek = diesel.size,
                maintenanceDueTitles = dueMaintenance.map { it.task.title },
                alreadyNotifiedMissingWeek = alreadyNotifiedMissing || !allowMissingWeek,
            )
            if (allowMissingWeek && plan.notifyMissingPaycheck) {
                notify(
                    applicationContext,
                    ID_PAYCHECK,
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_add_paycheck_title),
                    applicationContext.getString(R.string.notify_missing_week_body, lastWeek)
                )
            }
            if (allowMissingWeek && plan.notifyMissingDiesel) {
                notify(
                    applicationContext,
                    ID_DIESEL,
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_add_diesel_title),
                    applicationContext.getString(R.string.notify_missing_week_body, lastDieselWeek)
                )
            }
            if (allowMissingWeek && (plan.notifyMissingPaycheck || plan.notifyMissingDiesel)) {
                prefs.edit { putString(KEY_MISSING_WEEK, weekKey) }
            }
            if (plan.maintenanceDueTitles.isNotEmpty()) {
                val body = if (plan.maintenanceDueTitles.size == 1) {
                    applicationContext.getString(
                        R.string.notify_maintenance_body,
                        plan.maintenanceDueTitles.first(),
                    )
                } else {
                    applicationContext.getString(
                        R.string.notify_maintenance_body_multi,
                        plan.maintenanceDueTitles.size,
                        plan.maintenanceSummaryBody(),
                    )
                }
                notify(
                    applicationContext,
                    ID_MAINTENANCE_BUNDLE,
                    CHANNEL_ALERTS,
                    applicationContext.getString(R.string.notify_maintenance_title),
                    body,
                )
            }
            dueMaintenance.forEach { progress ->
                maintenanceRepo.markNotified(progress.task.id)
            }
            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("SmartNotification", "check failed", e)
            Result.retry()
        }
    }

    private fun cancelLegacyPerTaskMaintenanceAlerts() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Older builds posted one notification per due task (ids 100..N). Keep id 100 for the bundle.
        for (id in (LEGACY_MAINTENANCE_ID_START + 1)..LEGACY_MAINTENANCE_ID_END) {
            nm.cancel(id)
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_channel_missing_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { setShowBadge(true) }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    applicationContext.getString(R.string.notify_channel_alerts_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { setShowBadge(true) }
            )
        }
    }

    private fun notify(context: Context, id: Int, channel: String, title: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}
