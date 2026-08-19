package com.truckerload.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.truckerload.R
import com.truckerload.data.preferences.PushTokenStore
import com.truckerload.data.sync.cloud.SyncModeStore
import com.truckerload.data.voice.CallNotifications
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import com.truckerload.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TruckerLoadFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushTokenStore: PushTokenStore

    @Inject
    lateinit var syncModeStore: SyncModeStore

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        pushTokenStore.set(token)
        if (syncModeStore.allowsCloudCalls()) {
            PushTokenRegistrationWorker.enqueue(applicationContext)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"].orEmpty()
        if (type == "sync") {
            // FCM is a wake-up only; skip cloud workers in DEVICE_ONLY.
            if (syncModeStore.allowsCloudCalls()) {
                CloudSyncWorker.enqueue(applicationContext)
                MediaSyncWorker.enqueue(applicationContext)
                ServerTelegramInboxWorker.enqueue(applicationContext)
                CommunityInboxWorker.enqueue(applicationContext)
            }
            return
        }
        if (type == FriendsLocationSharePolicy.FCM_WATCH_TYPE) {
            FriendsLocationShareScheduler.startLiveSession(applicationContext, fromUserToggle = false)
            return
        }
        if (handleCallPush(type, message.data)) return
        showNotification(
            title = message.notification?.title ?: message.data["title"],
            body = message.notification?.body ?: message.data["body"],
            collapseKey = message.collapseKey
                ?: message.data["type"]
                ?: message.data["collapse_key"],
        )
    }

    private fun handleCallPush(type: String, data: Map<String, String>): Boolean {
        val calls = CallNotifications(this)
        when (type) {
            "incoming_call" -> {
                if (syncModeStore.allowsCloudCalls()) CommunityInboxWorker.enqueue(applicationContext)
                calls.showIncoming(
                    callId = data["callId"].orEmpty().ifBlank { "incoming" },
                    callerName = data["callerName"] ?: data["title"].orEmpty(),
                )
                return true
            }
            "missed_call" -> {
                calls.showMissed(
                    peerId = data["peerId"].orEmpty(),
                    peerName = data["peerName"] ?: data["callerName"].orEmpty(),
                )
                return true
            }
            "group_call" -> {
                if (syncModeStore.allowsCloudCalls()) CommunityInboxWorker.enqueue(applicationContext)
                calls.showGroupCall(
                    chatId = data["chatId"].orEmpty(),
                    title = data["title"] ?: data["groupName"].orEmpty(),
                )
                return true
            }
            else -> return false
        }
    }

    private fun showNotification(title: String?, body: String?, collapseKey: String?) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val safeTitle = title.safeText(getString(R.string.push_notification_fallback_title))
        val safeBody = body.safeText(getString(R.string.push_notification_fallback_body))
        // Stable id: same type/title replaces the previous shade entry instead of stacking.
        val notificationId = stableNotificationId(collapseKey, safeTitle)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(safeTitle)
            .setContentText(safeBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safeBody))
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
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
            ).apply { setShowBadge(true) },
        )
    }

    private fun String?.safeText(fallback: String): String =
        this?.trim()?.takeIf { it.isNotBlank() }?.take(MAX_NOTIFICATION_TEXT) ?: fallback

    private fun stableNotificationId(collapseKey: String?, title: String): Int {
        val seed = collapseKey?.takeIf { it.isNotBlank() } ?: title
        return (PUSH_ID_BASE + (seed.hashCode() and 0xffff)).coerceIn(PUSH_ID_BASE, PUSH_ID_BASE + 0xffff)
    }

    companion object {
        private const val CHANNEL_ID = "truckerload_updates"
        private const val GROUP_KEY = "truckerload_updates_group"
        private const val MAX_NOTIFICATION_TEXT = 240
        private const val PUSH_ID_BASE = 5000
    }
}
