package com.truckerload.data.community

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
import com.truckerload.R
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.navigation.Routes

class CommunityMessageNotifier(context: Context) {
    private val appContext = context.applicationContext

    fun notifyNewMessage(chatId: String, senderName: String, text: String, extraCount: Int = 1) {
        if (ActiveCommunityChat.chatId == chatId) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        val title = senderName.trim().ifBlank {
            appContext.getString(R.string.push_notification_fallback_title)
        }
        val body = when {
            extraCount > 1 -> appContext.getString(R.string.community_message_notification_more, extraCount)
            text.isNotBlank() -> text
            else -> appContext.getString(R.string.community_message_notification_empty)
        }
        val openChat = PendingIntent.getActivity(
            appContext,
            chatId.hashCode(),
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, Routes.socialChat(chatId))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body.take(MAX_TEXT))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(MAX_TEXT)))
            .setContentIntent(openChat)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId(chatId), notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.community_message_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { setShowBadge(true) },
        )
    }

    private fun notificationId(chatId: String): Int =
        NOTIFY_BASE + (chatId.hashCode() and 0xffff)

    companion object {
        private const val CHANNEL_ID = "truckerload_community_messages"
        private const val NOTIFY_BASE = 7100
        private const val MAX_TEXT = 240
    }
}
