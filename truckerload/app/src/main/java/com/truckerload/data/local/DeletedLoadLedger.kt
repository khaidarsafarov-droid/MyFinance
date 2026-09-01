package com.truckerload.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Durable record of loads the user deleted. Survives process death, Telegram
 * re-ingest, companion backup auto-restore, and cloud pull of a stale snapshot.
 */
object DeletedLoadLedger {
    private const val PREFS = "truckerload_deleted_loads"
    private const val KEY_LOAD_IDS = "load_ids"
    private const val KEY_TRIP_IDS = "trip_ids"
    private const val KEY_PENDING = "pending_ids"
    private const val KEY_PENDING_TRIPS = "pending_trips"

    fun markPending(context: Context, loadId: String, tripId: String?) {
        val id = loadId.trim()
        if (id.isBlank()) return
        val trip = tripId?.trim()?.takeIf { it.isNotBlank() }
        prefs(context).edit(commit = true) {
            putStringSet(KEY_PENDING, HashSet(pendingIds(context) + id))
            putStringSet(KEY_LOAD_IDS, HashSet(loadIds(context) + id))
            if (trip != null) {
                putStringSet(KEY_TRIP_IDS, HashSet(tripIds(context) + trip))
                putStringSet(KEY_PENDING_TRIPS, HashSet(pendingTrips(context) + "$id\t$trip"))
            }
        }
    }

    fun cancelPending(context: Context, loadId: String) {
        val id = loadId.trim()
        if (id.isBlank()) return
        val trip = pendingTripFor(context, id)
        prefs(context).edit(commit = true) {
            putStringSet(KEY_PENDING, HashSet(pendingIds(context) - id))
            putStringSet(KEY_LOAD_IDS, HashSet(loadIds(context) - id))
            putStringSet(KEY_PENDING_TRIPS, HashSet(pendingTrips(context).filterNot { it.startsWith("$id\t") }))
            if (trip != null) {
                putStringSet(KEY_TRIP_IDS, HashSet(tripIds(context) - trip))
            }
        }
    }

    fun markDeleted(context: Context, loadId: String, tripId: String?) {
        val id = loadId.trim()
        if (id.isBlank()) return
        prefs(context).edit(commit = true) {
            putStringSet(KEY_PENDING, HashSet(pendingIds(context) - id))
            putStringSet(KEY_LOAD_IDS, HashSet(loadIds(context) + id))
            tripId?.trim()?.takeIf { it.isNotBlank() }?.let { trip ->
                putStringSet(KEY_TRIP_IDS, HashSet(tripIds(context) + trip))
            }
        }
    }

    fun isBlocked(context: Context, loadId: String?, tripId: String?): Boolean {
        val id = loadId?.trim().orEmpty()
        val trip = tripId?.trim().orEmpty()
        if (id.isNotBlank() && id in loadIds(context)) return true
        if (trip.isNotBlank() && trip in tripIds(context)) return true
        return false
    }

    fun hasAny(context: Context): Boolean =
        loadIds(context).isNotEmpty() || tripIds(context).isNotEmpty() || pendingIds(context).isNotEmpty()

    fun pendingHardDeleteIds(context: Context): Set<String> = pendingIds(context)

    fun blockedLoadIds(context: Context): Set<String> = loadIds(context)

    fun blockedTripIds(context: Context): Set<String> = tripIds(context)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun pendingIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PENDING, emptySet()).orEmpty().toSet()

    private fun loadIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_LOAD_IDS, emptySet()).orEmpty().toSet()

    private fun tripIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_TRIP_IDS, emptySet()).orEmpty().toSet()

    private fun pendingTrips(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PENDING_TRIPS, emptySet()).orEmpty().toSet()

    private fun pendingTripFor(context: Context, loadId: String): String? =
        pendingTrips(context).firstOrNull { it.startsWith("$loadId\t") }?.substringAfter('\t')
}
