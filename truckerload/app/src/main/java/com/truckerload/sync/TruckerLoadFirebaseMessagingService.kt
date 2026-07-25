package com.truckerload.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.truckerload.R
import com.truckerload.data.preferences.PushTokenStore
import com.truckerload.presentation.MainActivity

class TruckerLoadFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushTokenStore(applicationContext).set(token)
        PushTokenRegistrationWorker.enqueue(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] == "sync") {
            CloudSyncWorker.enqueue(applicationContext)
            MediaSyncWorker.enqueue(applicationContext)
            ServerTelegramInboxWorker.enqueue(applicationContext)
            return
        }
        showNotification(
            title = message.notification?.title ?: message.data["title"],
            body = message.notification?.body ?: message.data["body"],
        )
    }

    private fun showNotification(title: String?, body: String?) {
        createChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.safeText(getString(R.string.push_notification_fallback_title)))
            .setContentText(body.safeText(getString(R.string.push_notification_fallback_body)))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(
                (System.currentTimeMillis() and 0x7fffffff).toInt(),
                notification,
            )
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.push_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun String?.safeText(fallback: String): String =
        this?.trim()?.takeIf { it.isNotBlank() }?.take(MAX_NOTIFICATION_TEXT) ?: fallback

    companion object {
        private const val CHANNEL_ID = "truckerload_updates"
        private const val MAX_NOTIFICATION_TEXT = 240
    }
}
