package com.truckerload.sync

import android.content.Context
import android.util.Log
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.FriendsLocationShareStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.UserProfileStore
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.friends.ActiveLoadSelector
import com.truckerload.domain.friends.BalancedLocationFix
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendsLocationSharePolicy
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.SharedLoadStatus
import com.truckerload.utils.LocationHelper
import java.util.ArrayDeque

/**
 * One GPS sample → compact presence upsert. Live mode may also refresh the active route.
 * Coordinates are never written to logs.
 */
class FriendsLocationSharePublisher(
    context: Context,
    private val auth: AuthStore = AuthStore(context),
    private val settings: SettingsDataStore = SettingsDataStore(context),
    private val runtime: FriendsLocationShareStore = FriendsLocationShareStore(context),
    private val locationHelper: LocationHelper = LocationHelper(context),
    private val api: SupabaseFriendsRealtimeService = SupabaseFriendsRealtimeService(auth),
) {
    private val app = context.applicationContext
    private val track = ArrayDeque<LatLngPoint>(MAX_TRACK)

    enum class Mode { PRESENCE_ONLY, LIVE }

    suspend fun publishOnce(mode: Mode): Boolean {
        if (!settings.getSharePathWithFriendsOnce() || !api.isConfigured()) {
            clearRemote()
            return false
        }
        if (!FriendsLocationShareService.hasLocationPermission(app)) return false
        val now = System.currentTimeMillis()
        if (mode == Mode.PRESENCE_ONLY &&
            FriendsLocationSharePolicy.shouldSkipStationaryFix(
                runtime.lastMotion(),
                runtime.lastPublishedAtMs(),
                now,
            )
        ) {
            return true
        }
        val fix = locationHelper.getBalancedCurrentLocation() ?: return false
        val ok = upsertPresence(fix)
        if (ok) runtime.markPublished(fix.timestampMillis)
        if (ok && mode == Mode.LIVE) {
            rememberTrack(fix)
            publishActiveRoute()
        }
        return ok
    }

    suspend fun clearRemote() {
        if (!api.isConfigured()) return
        runCatching { api.clearPresence() }
        runCatching { api.clearActiveRoute() }
    }

    private suspend fun upsertPresence(fix: BalancedLocationFix): Boolean {
        val profile = UserProfileStore(app).profile.value
        val name = listOfNotNull(profile?.givenName, profile?.familyName)
            .joinToString(" ")
            .ifBlank { profile?.email ?: "Driver" }
        val sendAccuracy = runtime.presenceAccuracySupported() != false
        val first = api.upsertPresence(
            displayName = name,
            lat = fix.latitude,
            lng = fix.longitude,
            sharePathEnabled = true,
            accuracyMeters = if (sendAccuracy) fix.accuracyMeters else null,
            timestampMillis = fix.timestampMillis,
        )
        if (first.isSuccess) {
            if (sendAccuracy) runtime.setPresenceAccuracySupported(true)
            return true
        }
        val msg = first.exceptionOrNull()?.message.orEmpty()
        if (sendAccuracy && msg.contains("accuracy_m")) {
            runtime.setPresenceAccuracySupported(false)
            return api.upsertPresence(
                displayName = name,
                lat = fix.latitude,
                lng = fix.longitude,
                sharePathEnabled = true,
                timestampMillis = fix.timestampMillis,
            ).isSuccess
        }
        Log.w(TAG, "presence upsert failed")
        return false
    }

    private fun rememberTrack(fix: BalancedLocationFix) {
        track.addLast(LatLngPoint(fix.latitude, fix.longitude))
        while (track.size > MAX_TRACK) track.removeFirst()
    }

    private suspend fun publishActiveRoute() {
        val db = AppDatabase.getInstanceForActiveUser(app) ?: return
        val loads = LoadRepository(db).getAllLoadsOnce()
        val active = ActiveLoadSelector.selectActive(loads)
        if (active == null) {
            api.clearActiveRoute()
            return
        }
        val originLabel = active.pointA.ifBlank { active.firstPuCityState }
        val destLabel = active.pointB.ifBlank { active.lastDelCityState }
        val origin = locationHelper.geocodeAddress(originLabel) ?: track.firstOrNull()
        val destination = locationHelper.geocodeAddress(destLabel)
        api.upsertActiveRoute(
            FriendActiveRoute(
                userId = auth.currentUserIdOrNull().orEmpty(),
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

    companion object {
        private const val TAG = "FriendsLocationShare"
        private const val MAX_TRACK = 200
    }
}
