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
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.repository.DieselRepository
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
    }

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val paycheckRepo = PaycheckRepository(db)
        val dieselRepo = DieselRepository(db)

        createChannels()
        val (currentWeek, year) = getCurrentWeekNumberAndYear()
        val (lastWeek, lastYear) = shiftWeekNumberAndYear(currentWeek, year, -1)

        return try {
            val paycheck = paycheckRepo.getPaycheckForWeek(lastWeek, lastYear)
            if (paycheck == null) {
                notify(
                    applicationContext,
                    1,
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_add_paycheck_title),
                    applicationContext.getString(R.string.notify_missing_week_body, lastWeek)
                )
            }

            val diesel = dieselRepo.getDieselForWeek(lastWeek, lastYear).first()
            if (diesel.isEmpty()) {
                notify(
                    applicationContext,
                    2,
                    CHANNEL_MISSING,
                    applicationContext.getString(R.string.notify_add_diesel_title),
                    applicationContext.getString(R.string.notify_missing_week_body, lastWeek)
                )
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
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    applicationContext.getString(R.string.notify_channel_alerts_name),
                    NotificationManager.IMPORTANCE_DEFAULT
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}
