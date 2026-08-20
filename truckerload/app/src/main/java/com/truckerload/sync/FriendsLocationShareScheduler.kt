package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.FriendsLocationShareStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Chooses WorkManager vs time-boxed FGS for friends location sharing.
 */
object FriendsLocationShareScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mapVisible = AtomicBoolean(false)

    fun isMapVisible(): Boolean = mapVisible.get()

    fun sync(context: Context) {
        val app = context.applicationContext
        scope.launch { syncBlocking(app) }
    }

    fun onFriendsMapOpened(context: Context) {
        val app = context.applicationContext
        mapVisible.set(true)
        scope.launch {
            FriendsLocationShareStore(app).setLiveUntilMs(
                FriendsLocationSharePolicy.liveUntilFromNow(System.currentTimeMillis()),
            )
            syncBlocking(app)
        }
    }

    fun onFriendsMapClosed(context: Context) {
        val app = context.applicationContext
        mapVisible.set(false)
        scope.launch {
            val settings = SettingsDataStore(app)
            if (!settings.getFriendsLiveModeOnce()) {
                FriendsLocationShareStore(app).clearLiveUntil()
            }
            syncBlocking(app)
        }
    }

    /** 15-minute live burst (settings toggle or FCM `friends_watch`). */
    fun startLiveSession(context: Context, fromUserToggle: Boolean) {
        val app = context.applicationContext
        scope.launch {
            val settings = SettingsDataStore(app)
            if (!settings.getSharePathWithFriendsOnce()) return@launch
            FriendsLocationShareStore(app).setLiveUntilMs(
                FriendsLocationSharePolicy.liveUntilFromNow(System.currentTimeMillis()),
            )
            if (fromUserToggle) settings.saveFriendsLiveMode(true)
            syncBlocking(app)
        }
    }

    fun stopLiveSession(context: Context) {
        val app = context.applicationContext
        scope.launch {
            FriendsLocationShareStore(app).clearLiveUntil()
            SettingsDataStore(app).saveFriendsLiveMode(false)
            syncBlocking(app)
        }
    }

    suspend fun stopAndClear(context: Context) {
        val app = context.applicationContext
        mapVisible.set(false)
        FriendsLocationShareStore(app).clearLiveUntil()
        FriendsLocationShareWorker.cancel(app)
        FriendsActivityRecognition.unregister(app)
        FriendsLocationShareService.stopForLogout(app)
    }

    internal suspend fun syncBlocking(app: Context) {
        val settings = SettingsDataStore(app)
        val runtime = FriendsLocationShareStore(app)
        val sharing = settings.getSharePathWithFriendsOnce()
        if (!sharing) {
            FriendsLocationShareWorker.cancel(app)
            FriendsActivityRecognition.unregister(app)
            runtime.clearLiveUntil()
            FriendsLocationSharePublisher(app).clearRemote()
            FriendsLocationShareService.stop(app)
            return
        }
        FriendsActivityRecognition.register(app)
        val interval = FriendsLocationShareWorker.scheduledIntervalMinutes(settings, runtime)
        FriendsLocationShareWorker.enqueuePeriodic(app, interval)

        val livePref = settings.getFriendsLiveModeOnce()
        val liveTimed = runtime.isLiveSessionActive()
        if (livePref && !liveTimed && !mapVisible.get()) {
            settings.saveFriendsLiveMode(false)
        }
        val wantLive = mapVisible.get() || liveTimed
        if (wantLive) {
            FriendsLocationShareService.start(app)
        } else {
            FriendsLocationShareService.stop(app)
        }
    }
}
