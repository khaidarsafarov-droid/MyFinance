package com.truckerload.sync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.truckerload.R
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.FriendsLocationShareStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.CrashReporting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Time-boxed live sharing only. Background presence uses [FriendsLocationShareWorker].
 */
class FriendsLocationShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                running.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        ensureNotificationChannel()
        if (!hasLocationPermission(this)) {
            Log.w(TAG, "No location permission — satisfying FGS contract then stopping")
            promoteForeground(allowLocationType = false)
            running.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val promoted = promoteForeground(allowLocationType = true)
        if (!promoted) {
            running.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        running.set(true)
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { liveLoop() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running.set(false)
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun liveLoop() {
        val settings = SettingsDataStore(this)
        val runtime = FriendsLocationShareStore(this)
        val publisher = FriendsLocationSharePublisher(this)
        while (scope.isActive) {
            val sharing = settings.getSharePathWithFriendsOnce()
            val keepLive = FriendsLocationShareScheduler.isMapVisible() ||
                runtime.isLiveSessionActive()
            if (!sharing || !keepLive) {
                if (settings.getFriendsLiveModeOnce() && !runtime.isLiveSessionActive()) {
                    settings.saveFriendsLiveMode(false)
                }
                running.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            publisher.publishOnce(FriendsLocationSharePublisher.Mode.LIVE)
            delay(FriendsLocationSharePolicy.LIVE_GPS_PERIOD_MS)
        }
    }

    private fun promoteForeground(allowLocationType: Boolean): Boolean {
        val notification = buildNotification()
        if (allowLocationType && Build.VERSION.SDK_INT >= 34) {
            val ok = runCatching {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            }.onFailure { e ->
                Log.e(TAG, "startForeground(location) failed", e)
                CrashReporting.recordException(e)
            }.isSuccess
            if (ok) return true
        }
        return runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= 34) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        }.onFailure { e ->
            Log.e(TAG, "startForeground(dataSync) failed", e)
            CrashReporting.recordException(e)
        }.isSuccess
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.friends_share_location_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.friends_share_location_title))
            .setContentText(getString(R.string.friends_share_location_live_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.truckerload.STOP_FRIENDS_LOCATION_SHARE"
        private const val TAG = "FriendsLocationShare"
        private const val CHANNEL_ID = "friends_location_share"
        private const val NOTIFICATION_ID = 4721
        private val running = AtomicBoolean(false)

        fun isRunning(): Boolean = running.get()

        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun start(context: Context) {
            if (!hasLocationPermission(context)) {
                Log.w(TAG, "start skipped — location permission missing")
                return
            }
            val i = Intent(context, FriendsLocationShareService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot start friends location FGS", e)
                CrashReporting.recordException(e)
            }
        }

        fun stop(context: Context) {
            running.set(false)
            try {
                val i = Intent(context, FriendsLocationShareService::class.java).setAction(ACTION_STOP)
                context.startService(i)
            } catch (e: Exception) {
                Log.w(TAG, "stop via intent failed, falling back to stopService", e)
                runCatching {
                    context.stopService(Intent(context, FriendsLocationShareService::class.java))
                }
            }
        }

        suspend fun stopForLogout(context: Context) {
            val app = context.applicationContext
            withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseFriendsRealtimeService(AuthStore(app))
                    if (api.isConfigured()) {
                        api.clearPresence()
                        api.clearActiveRoute()
                    }
                }.onFailure {
                    Log.w(TAG, "clear presence on logout failed")
                }
            }
            stop(app)
        }
    }
}
