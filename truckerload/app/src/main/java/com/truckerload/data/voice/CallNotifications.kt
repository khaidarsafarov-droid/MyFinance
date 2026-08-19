package com.truckerload.data.voice

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

class CallNotifications(context: Context) {
    private val app = context.applicationContext

    fun showIncoming(callId: String, callerName: String) {
        val open = activityIntent(Routes.call(callId), requestCode = callId.hashCode())
        notify(
            id = incomingId(),
            title = callerName.ifBlank { app.getString(R.string.incoming_call) },
            body = app.getString(R.string.call_incoming_body),
            category = NotificationCompat.CATEGORY_CALL,
            fullScreen = open,
            content = open,
            ongoing = true,
        )
    }

    fun showMissed(peerId: String, peerName: String) {
        val openProfile = activityIntent(Routes.peerProfile(peerId), requestCode = peerId.hashCode())
        val redial = activityIntent(Routes.peerProfile(peerId), requestCode = peerId.hashCode() + 1)
        notify(
            id = missedId(peerId),
            title = app.getString(R.string.call_missed),
            body = app.getString(R.string.call_missed_body, peerName.ifBlank { peerId }),
            category = NotificationCompat.CATEGORY_MISSED_CALL,
            content = openProfile,
            actionLabel = app.getString(R.string.call_redial),
            actionIntent = redial,
        )
        cancelIncoming()
    }

    fun showGroupCall(chatId: String, title: String) {
        notify(
            id = groupId(chatId),
            title = title.ifBlank { app.getString(R.string.group_start_call) },
            body = app.getString(R.string.group_call_banner),
            category = NotificationCompat.CATEGORY_CALL,
            content = activityIntent(Routes.socialChat(chatId), requestCode = chatId.hashCode()),
        )
    }

    fun cancelIncoming() {
        runCatching { NotificationManagerCompat.from(app).cancel(INCOMING_ID) }
    }

    private fun notify(
        id: Int,
        title: String,
        body: String,
        category: String,
        content: PendingIntent,
        fullScreen: PendingIntent? = null,
        ongoing: Boolean = false,
        actionLabel: String? = null,
        actionIntent: PendingIntent? = null,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.take(MAX_TEXT))
            .setContentText(body.take(MAX_TEXT))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(MAX_TEXT)))
            .setContentIntent(content)
            .setCategory(category)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setOngoing(ongoing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (fullScreen != null) {
            builder.setFullScreenIntent(fullScreen, true)
        }
        if (actionLabel != null && actionIntent != null) {
            builder.addAction(0, actionLabel, actionIntent)
        }
        runCatching { NotificationManagerCompat.from(app).notify(id, builder.build()) }
    }

    private fun activityIntent(route: String, requestCode: Int): PendingIntent {
        val intent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            app,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = app.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                app.getString(R.string.call_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setShowBadge(true)
                enableVibration(true)
            },
        )
    }

    private fun incomingId(): Int = INCOMING_ID
    private fun missedId(peerId: String): Int = MISSED_BASE + (peerId.hashCode() and 0xffff)
    private fun groupId(chatId: String): Int = GROUP_BASE + (chatId.hashCode() and 0xffff)

    companion object {
        private const val CHANNEL_ID = "truckerload_calls"
        private const val INCOMING_ID = 7601
        private const val MISSED_BASE = 7700
        private const val GROUP_BASE = 7800
        private const val MAX_TEXT = 240
    }
}
