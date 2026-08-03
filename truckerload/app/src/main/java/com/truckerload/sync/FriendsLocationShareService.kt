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
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.SharedLoadStatus
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.CrashReporting
import com.truckerload.utils.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Publishes GPS presence + active route to Supabase while privacy toggle is ON.
 * Stops publishing (and clears presence) when privacy is OFF.
 *
 * Uses [START_NOT_STICKY]: a location-type FGS must not be auto-restarted from the
 * background on Android 14+ (SecurityException → "keeps stopping" dialog). The friends
 * map UI restarts sharing when the user returns with the toggle still on.
 */
class FriendsLocationShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    clearRemote()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
        }
        ensureNotificationChannel()
        if (!hasLocationPermission(this)) {
            Log.w(TAG, "No location permission — satisfying FGS contract then stopping")
            // Still call startForeground so startForegroundService() does not crash the app.
            promoteForeground(allowLocationType = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val promoted = promoteForeground(allowLocationType = true)
        if (!promoted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { publishLoop() }
        }
        // Do not sticky-restart from background — location FGS is while-in-use only.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun publishLoop() {
        val settings = SettingsDataStore(this)
        val auth = AuthStore(this)
        val api = SupabaseFriendsRealtimeService(auth)
        val locationHelper = LocationHelper(this)
        val track = ArrayList<LatLngPoint>()
        while (scope.isActive) {
            val sharing = settings.getSharePathWithFriendsOnce()
            if (!sharing || !api.isConfigured()) {
                clearRemote()
                delay(15_000)
                continue
            }
            val loc = locationHelper.getCurrentLocation()
            val lat = loc?.latitude
            val lng = loc?.longitude
            if (lat != null && lng != null) {
                val point = LatLngPoint(lat, lng)
                track += point
                if (track.size > 200) track.removeAt(0)
                val profile = UserProfileStore(this).profile.value
                val name = listOfNotNull(profile?.givenName, profile?.familyName)
                    .joinToString(" ")
                    .ifBlank { profile?.email ?: "Driver" }
                api.upsertPresence(
                    displayName = name,
                    lat = lat,
                    lng = lng,
                    sharePathEnabled = true,
                )
                publishActiveRoute(api, track)
            }
            delay(30_000)
        }
    }

    private suspend fun publishActiveRoute(
        api: SupabaseFriendsRealtimeService,
        track: List<LatLngPoint>,
    ) {
        val db = AppDatabase.getInstanceForActiveUser(this) ?: return
        val loads = LoadRepository(db).getAllLoadsOnce()
        val active = ActiveLoadSelector.selectActive(loads)
        if (active == null) {
            // Finished / no in-progress load — remove stale route from friends' maps.
            api.clearActiveRoute()
            return
        }
        val originLabel = active.pointA.ifBlank { active.firstPuCityState }
        val destLabel = active.pointB.ifBlank { active.lastDelCityState }
        val helper = LocationHelper(this)
        val geocodedOrigin = helper.geocodeAddress(originLabel)
        val geocodedDest = helper.geocodeAddress(destLabel)
        val origin = geocodedOrigin ?: track.firstOrNull()
        val destination = geocodedDest
        api.upsertActiveRoute(
            FriendActiveRoute(
                userId = AuthStore(this).currentUserIdOrNull().orEmpty(),
                displayName = "",
                loadRef = active.id,
                originLabel = originLabel,
                destinationLabel = destLabel,
                origin = origin,
                destination = destination,
                startDate = ActiveLoadSelector.startDateIso(active),
                endDate = ActiveLoadSelector.endDateIso(active),
                status = SharedLoadStatus.ACTIVE,
                trackPoints = track.toList(),
            ),
            sharePathEnabled = true,
        )
    }

    private suspend fun clearRemote() {
        val api = SupabaseFriendsRealtimeService(AuthStore(this))
        if (api.isConfigured()) {
            api.clearPresence()
            api.clearActiveRoute()
        }
    }

    /**
     * @return true if [ServiceCompat.startForeground] succeeded.
     * When [allowLocationType] is false (or location start fails), falls back to dataSync
     * so the startForegroundService() contract is still met.
     */
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
            .setContentText(getString(R.string.friends_share_location_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.truckerload.STOP_FRIENDS_LOCATION_SHARE"
        private const val TAG = "FriendsLocationShare"
        private const val CHANNEL_ID = "friends_location_share"
        private const val NOTIFICATION_ID = 4721

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
            try {
                val i = Intent(context, FriendsLocationShareService::class.java).setAction(ACTION_STOP)
                context.startService(i)
            } catch (e: Exception) {
                // App may be backgrounded; force-stop if startService is blocked.
                Log.w(TAG, "stop via intent failed, falling back to stopService", e)
                runCatching {
                    context.stopService(Intent(context, FriendsLocationShareService::class.java))
                }
            }
        }

        /**
         * Clears remote presence/route while auth is still valid, then stops the FGS.
         * Call from logout **before** wiping tokens.
         */
        suspend fun stopForLogout(context: Context) {
            val app = context.applicationContext
            // FIX: logout previously left FGS running and stale presence visible to friends
            withContext(Dispatchers.IO) {
                runCatching {
                    val api = SupabaseFriendsRealtimeService(AuthStore(app))
                    if (api.isConfigured()) {
                        api.clearPresence()
                        api.clearActiveRoute()
                    }
                }.onFailure { e ->
                    Log.w(TAG, "clear presence on logout failed", e)
                }
            }
            stop(app)
        }
    }
}
