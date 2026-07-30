package com.truckerload.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
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
import com.truckerload.presentation.MainActivity
import com.truckerload.utils.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Publishes GPS presence + active route to Supabase while privacy toggle is ON.
 * Stops publishing (and clears presence + active route) when privacy is OFF.
 */
class FriendsLocationShareService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    // FIX: serialize publish vs clear to avoid re-publishing after share OFF
    private val publishMutex = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    // FIX: stop the loop before clearing remote state
                    loopJob?.cancelAndJoin()
                    loopJob = null
                    clearRemote()
                    stopSelf()
                }
                return START_NOT_STICKY
            }
        }
        ensureNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { publishLoop() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        val appCtx = applicationContext
        // FIX: clear remote even if the service scope is about to cancel
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                clearRemoteStatic(appCtx)
            }
        }
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
                publishMutex.withLock {
                    api.upsertPresence(
                        displayName = name,
                        lat = lat,
                        lng = lng,
                        sharePathEnabled = true,
                    )
                    publishActiveRoute(api, track)
                }
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
        val active = ActiveLoadSelector.selectForMapRoute(loads)
        // FIX: clear stale route when no active/future load remains
        if (active == null) {
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
                status = ActiveLoadSelector.statusFor(active),
                trackPoints = track.toList(),
            ),
            sharePathEnabled = true,
        )
    }

    private suspend fun clearRemote() {
        publishMutex.withLock {
            clearRemoteStatic(this)
        }
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
        private const val CHANNEL_ID = "friends_location_share"
        private const val NOTIFICATION_ID = 4721

        fun start(context: Context) {
            val i = Intent(context, FriendsLocationShareService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            val i = Intent(context, FriendsLocationShareService::class.java).setAction(ACTION_STOP)
            context.startService(i)
        }

        /** Clears presence + active route shares for the signed-in user. */
        suspend fun clearRemoteStatic(context: Context) {
            val api = SupabaseFriendsRealtimeService(AuthStore(context))
            if (api.isConfigured()) {
                api.clearPresence()
                // FIX: also revoke active_route_shares so friends stop seeing the path
                api.clearActiveRoute()
            }
        }
    }
}
