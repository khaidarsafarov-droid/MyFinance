package com.truckerload.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.MaintenanceRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.shiftWeekNumberAndYear
import kotlinx.coroutines.flow.first

class SmartNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_MISSING = "truckerload_missing"
        const val CHANNEL_ALERTS = "truckerload_alerts"
        private const val MISSING_DATA_NOTIFICATION_ID = 1
        private const val MAINTENANCE_NOTIFICATION_ID = 2
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstanceForActiveUser(applicationContext)
            ?: return Result.success()
        val paycheckRepo = PaycheckRepository(db)
        val dieselRepo = DieselRepository(db)
        val maintenanceRepo = MaintenanceRepository(db)

        createChannels()
        val (currentWeek, year) = getCurrentWeekNumberAndYear()
        val (lastWeek, lastYear) = shiftWeekNumberAndYear(currentWeek, year, -1)

        return try {
            val paycheck = paycheckRepo.getPaycheckForWeek(lastWeek, lastYear)
            val diesel = dieselRepo.getDieselForWeek(lastWeek, lastYear).first()
            val dueMaintenance = maintenanceRepo.getDueProgressForNotifications()
            val plan = SmartNotificationPlanner.plan(
                hasPaycheckForLastWeek = paycheck != null,
                dieselEntriesLastWeek = diesel.size,
                maintenanceDueTitles = dueMaintenance.map { it.task.title },
            )
            val missingParts = buildList {
                if (plan.notifyMissingPaycheck) add(applicationContext.getString(R.string.notify_add_paycheck_title))
                if (plan.notifyMissingDiesel) add(applicationContext.getString(R.string.notify_add_diesel_title))
            }
            if (missingParts.isNotEmpty()) {
                notify(
                    applicationContext,
                    MISSING_DATA_NOTIFICATION_ID,
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_missing_data_title),
                    applicationContext.getString(
                        R.string.notify_missing_data_body,
                        lastWeek,
                        missingParts.joinToString(", "),
                    ),
                )
            }
            if (plan.maintenanceDueTitles.isNotEmpty()) {
                notify(
                    applicationContext,
                    MAINTENANCE_NOTIFICATION_ID,
                    CHANNEL_ALERTS,
                    applicationContext.getString(R.string.notify_maintenance_title),
                    plan.maintenanceDueTitles.joinToString("\n") { "• $it" },
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

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_channel_missing_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    applicationContext.getString(R.string.notify_channel_alerts_name),
                    NotificationManager.IMPORTANCE_LOW
                )
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}
