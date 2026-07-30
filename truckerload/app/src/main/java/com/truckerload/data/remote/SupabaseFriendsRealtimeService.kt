package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthStore
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendPresence
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.SharedLoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * REST client for friends presence + active route shares on Supabase.
 * Realtime can be layered later; polling works with anon key + user JWT.
 */
class SupabaseFriendsRealtimeService(
    private val authStore: AuthStore,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean =
        !BuildConfig.LOCAL_ONLY_MODE &&
            BuildConfig.SUPABASE_URL.isNotBlank() &&
            BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    suspend fun upsertPresence(
        displayName: String,
        lat: Double,
        lng: Double,
        sharePathEnabled: Boolean,
        heading: Double? = null,
        speedMps: Double? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull() ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        val body = JSONObject()
            .put("user_id", userId)
            .put("display_name", displayName)
            .put("latitude", lat)
            .put("longitude", lng)
            .put("share_path_enabled", sharePathEnabled)
            .put("updated_at", Instant.now().toString())
        if (heading != null) body.put("heading", heading)
        if (speedMps != null) body.put("speed_mps", speedMps)
        upsert("driver_presence", body, token)
    }

    suspend fun clearPresence(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull() ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        deleteEq("driver_presence", "user_id", userId, token)
    }

    suspend fun upsertActiveRoute(route: FriendActiveRoute, sharePathEnabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val userId = authStore.currentUserIdOrNull() ?: return@withContext Result.failure(IllegalStateException("no user"))
            val token = authStore.accessTokenOrNull().orEmpty()
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
            val points = JSONArray()
            route.trackPoints.forEach { p ->
                points.put(JSONObject().put("lat", p.lat).put("lng", p.lng))
            }
            val body = JSONObject()
                .put("user_id", userId)
                .put("load_ref", route.loadRef)
                .put("origin_label", route.originLabel)
                .put("destination_label", route.destinationLabel)
                .put("origin_lat", route.origin?.lat)
                .put("origin_lng", route.origin?.lng)
                .put("dest_lat", route.destination?.lat)
                .put("dest_lng", route.destination?.lng)
                .put("start_date", route.startDate.take(10))
                .put("end_date", route.endDate.take(10))
                .put("status", when (route.status) {
                    SharedLoadStatus.ACTIVE -> "active"
                    SharedLoadStatus.COMPLETED -> "completed"
                    SharedLoadStatus.FUTURE -> "active"
                    SharedLoadStatus.UNKNOWN -> "active"
                })
                .put("track_points", points)
                .put("share_path_enabled", sharePathEnabled)
                .put("updated_at", Instant.now().toString())
            upsert("active_route_shares", body, token)
        }

    suspend fun fetchFriendPresence(): Result<List<FriendPresence>> = withContext(Dispatchers.IO) {
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        val json = get("driver_presence?select=*&share_path_enabled=eq.true&order=updated_at.desc", token)
            .getOrElse { return@withContext Result.failure(it) }
        val arr = JSONArray(json)
        val me = authStore.currentUserIdOrNull()
        val out = ArrayList<FriendPresence>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uid = o.optString("user_id")
            if (uid == me) continue
            out += FriendPresence(
                userId = uid,
                displayName = o.optString("display_name").ifBlank { "Driver" },
                latitude = o.optDouble("latitude"),
                longitude = o.optDouble("longitude"),
                updatedAtMillis = parseInstantMillis(o.optString("updated_at")),
                sharePathEnabled = o.optBoolean("share_path_enabled", true),
            )
        }
        Result.success(out)
    }

    suspend fun fetchFriendActiveRoutes(): Result<List<FriendActiveRoute>> = withContext(Dispatchers.IO) {
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        val json = get(
            "active_route_shares?select=*&status=eq.active&share_path_enabled=eq.true",
            token,
        ).getOrElse { return@withContext Result.failure(it) }
        val arr = JSONArray(json)
        val me = authStore.currentUserIdOrNull()
        val out = ArrayList<FriendActiveRoute>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uid = o.optString("user_id")
            if (uid == me) continue
            val trackArr = o.optJSONArray("track_points") ?: JSONArray()
            val track = buildList {
                for (j in 0 until trackArr.length()) {
                    val p = trackArr.optJSONObject(j) ?: continue
                    add(LatLngPoint(p.optDouble("lat"), p.optDouble("lng")))
                }
            }
            val oLat = o.optDouble("origin_lat", Double.NaN)
            val oLng = o.optDouble("origin_lng", Double.NaN)
            val dLat = o.optDouble("dest_lat", Double.NaN)
            val dLng = o.optDouble("dest_lng", Double.NaN)
            out += FriendActiveRoute(
                userId = uid,
                displayName = "Driver",
                loadRef = o.optString("load_ref").takeIf { it.isNotBlank() },
                originLabel = o.optString("origin_label"),
                destinationLabel = o.optString("destination_label"),
                origin = if (!oLat.isNaN() && !oLng.isNaN()) LatLngPoint(oLat, oLng) else null,
                destination = if (!dLat.isNaN() && !dLng.isNaN()) LatLngPoint(dLat, dLng) else null,
                startDate = o.optString("start_date"),
                endDate = o.optString("end_date"),
                status = SharedLoadStatus.ACTIVE,
                trackPoints = track,
            )
        }
        Result.success(out)
    }

    private fun upsert(table: String, body: JSONObject, token: String): Result<Unit> {
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Prefer", "resolution=merge-duplicates")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("upsert $table HTTP ${resp.code}: ${resp.body?.string()}")
            }
        }
    }

    private fun deleteEq(table: String, column: String, value: String, token: String): Result<Unit> {
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table?$column=eq.$value")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("delete $table HTTP ${resp.code}")
            }
        }
    }

    private fun get(pathAndQuery: String, token: String): Result<String> {
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$pathAndQuery")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("GET $pathAndQuery HTTP ${resp.code}: $body")
                body
            }
        }
    }

    private fun parseInstantMillis(raw: String): Long =
        runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
}
