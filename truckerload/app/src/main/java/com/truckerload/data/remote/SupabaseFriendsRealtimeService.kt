package com.truckerload.data.remote

import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthStore
import com.truckerload.domain.friends.FriendActiveRoute
import com.truckerload.domain.friends.FriendPresence
import com.truckerload.domain.friends.FriendProfileHit
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.domain.friends.NicknameValidator
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
 * REST client for friends presence, nicknames, and share links on Supabase.
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

    suspend fun upsertMyNickname(nickname: String, fullName: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val userId = authStore.currentUserIdOrNull()
                ?: return@withContext Result.failure(IllegalStateException("no user"))
            val token = authStore.accessTokenOrNull().orEmpty()
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
            val handle = NicknameValidator.sanitizeOrNull(nickname)
                ?: return@withContext Result.failure(IllegalArgumentException("invalid nickname"))
            val body = JSONObject()
                .put("id", userId)
                .put("nickname", handle)
            if (!fullName.isNullOrBlank()) body.put("full_name", fullName)
            upsert("profiles", body, token, onConflict = "id").mapSchemaErrors()
        }

    suspend fun searchByNickname(nickname: String): Result<FriendProfileHit?> =
        withContext(Dispatchers.IO) {
            val token = authStore.accessTokenOrNull().orEmpty()
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
            val handle = NicknameValidator.sanitizeOrNull(nickname)
                ?: return@withContext Result.success(null)
            val body = JSONObject().put("p_nickname", handle).toString()
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/search_profile_by_nickname")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            runCatching {
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("search nickname HTTP ${resp.code}: $text")
                    val arr = JSONArray(text)
                    if (arr.length() == 0) return@runCatching null
                    val o = arr.getJSONObject(0)
                    FriendProfileHit(
                        userId = o.optString("user_id"),
                        nickname = o.optString("nickname"),
                        displayName = o.optString("full_name").ifBlank { o.optString("nickname") },
                    )
                }
            }
        }

    suspend fun listMyFriendLinks(): Result<List<FriendShareLink>> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull()
            ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        // Prefer RPC that joins live profiles.nickname so friends see renames immediately.
        val fromRpc = runCatching {
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/list_my_friend_links")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("list_my_friend_links HTTP ${resp.code}: $text")
                parseFriendLinksJson(text)
            }
        }
        if (fromRpc.isSuccess) return@withContext fromRpc
        val json = get(
            "friend_links?select=*&owner_id=eq.$userId&order=created_at.desc",
            token,
        ).getOrElse { return@withContext Result.failure(it) }
        Result.success(parseFriendLinksJson(json))
    }

    private fun parseFriendLinksJson(json: String): List<FriendShareLink> {
        val arr = JSONArray(json)
        val out = ArrayList<FriendShareLink>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += FriendShareLink(
                friendUserId = o.optString("friend_id"),
                friendNickname = o.optString("friend_nickname"),
                friendDisplayName = o.optString("friend_display_name"),
                shareMyLocation = o.optBoolean("share_my_location", true),
                shareMyRoute = o.optBoolean("share_my_route", true),
            )
        }
        return out
    }

    suspend fun addFriend(hit: FriendProfileHit): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull()
            ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        if (hit.userId == userId) {
            return@withContext Result.failure(IllegalArgumentException("cannot add yourself"))
        }
        val link = JSONObject()
            .put("owner_id", userId)
            .put("friend_id", hit.userId)
            .put("friend_nickname", hit.nickname)
            .put("friend_display_name", hit.displayName)
            .put("share_my_location", true)
            .put("share_my_route", true)
            .put("updated_at", Instant.now().toString())
        upsert("friend_links", link, token, onConflict = "owner_id,friend_id").getOrElse {
            return@withContext Result.failure(it)
        }
        // Also register follow edge for graph tooling / legacy RLS
        val friendship = JSONObject()
            .put("follower_id", userId)
            .put("followee_id", hit.userId)
        upsert("friendships", friendship, token, onConflict = "follower_id,followee_id")
        Result.success(Unit)
    }

    suspend fun updateFriendShare(
        friendUserId: String,
        shareMyLocation: Boolean,
        shareMyRoute: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull()
            ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        val body = JSONObject()
            .put("share_my_location", shareMyLocation)
            .put("share_my_route", shareMyRoute)
            .put("updated_at", Instant.now().toString())
        patchEq(
            table = "friend_links",
            filter = "owner_id=eq.$userId&friend_id=eq.$friendUserId",
            body = body,
            token = token,
        )
    }

    suspend fun removeFriend(friendUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull()
            ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        deleteFilter("friend_links", "owner_id=eq.$userId&friend_id=eq.$friendUserId", token)
            .getOrElse { return@withContext Result.failure(it) }
        deleteFilter("friendships", "follower_id=eq.$userId&followee_id=eq.$friendUserId", token)
        Result.success(Unit)
    }

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

    /** Remove published live route when the driver has no ACTIVE load left. */
    suspend fun clearActiveRoute(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = authStore.currentUserIdOrNull() ?: return@withContext Result.failure(IllegalStateException("no user"))
        val token = authStore.accessTokenOrNull().orEmpty()
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
        deleteEq("active_route_shares", "user_id", userId, token)
    }

    suspend fun upsertActiveRoute(route: FriendActiveRoute, sharePathEnabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val userId = authStore.currentUserIdOrNull() ?: return@withContext Result.failure(IllegalStateException("no user"))
            val token = authStore.accessTokenOrNull().orEmpty()
            if (token.isBlank()) return@withContext Result.failure(IllegalStateException("no token"))
            // Never advertise FUTURE / UNKNOWN as a live "active" route for friends.
            if (route.status != SharedLoadStatus.ACTIVE) {
                return@withContext clearActiveRoute()
            }
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
                .put("status", "active")
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

    private fun upsert(
        table: String,
        body: JSONObject,
        token: String,
        onConflict: String? = null,
    ): Result<Unit> {
        val prefer = if (onConflict.isNullOrBlank()) {
            "resolution=merge-duplicates"
        } else {
            "resolution=merge-duplicates,on_conflict=$onConflict"
        }
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("Prefer", prefer)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("upsert $table HTTP ${resp.code}: $text")
            }
        }
    }

    private fun Result<Unit>.mapSchemaErrors(): Result<Unit> =
        fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { err ->
                val msg = err.message.orEmpty()
                if (isMissingNicknameColumnError(msg)) {
                    Result.failure(IllegalStateException(ERROR_NICKNAME_SCHEMA_MISSING))
                } else {
                    Result.failure(err)
                }
            },
        )

    companion object {
        /** UI maps this to [R.string.friends_nickname_schema_missing]. */
        const val ERROR_NICKNAME_SCHEMA_MISSING = "schema_nickname_missing"

        fun isMissingNicknameColumnError(message: String): Boolean {
            val m = message.lowercase()
            return m.contains("pgrst204") ||
                (m.contains("nickname") && m.contains("schema cache")) ||
                (m.contains("nickname") && m.contains("could not find"))
        }
    }

    private fun patchEq(
        table: String,
        filter: String,
        body: JSONObject,
        token: String,
    ): Result<Unit> {
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table?$filter")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .patch(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("patch $table HTTP ${resp.code}: ${resp.body?.string()}")
            }
        }
    }

    private fun deleteEq(table: String, column: String, value: String, token: String): Result<Unit> =
        deleteFilter(table, "$column=eq.$value", token)

    private fun deleteFilter(table: String, filter: String, token: String): Result<Unit> {
        val req = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/$table?$filter")
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
