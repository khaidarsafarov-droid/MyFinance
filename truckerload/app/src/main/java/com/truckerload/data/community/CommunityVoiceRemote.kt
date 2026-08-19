package com.truckerload.data.community

import com.truckerload.data.preferences.AuthStore
import com.truckerload.domain.voice.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CommunityVoiceRemote(
    authStore: AuthStore,
) {
    private val rest = CommunityRestClient(authStore)

    fun isReady(): Boolean = rest.isReady()

    suspend fun listRooms(): Result<List<RemoteVoiceRoom>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.get("community_voice_rooms?select=*&is_active=eq.true&order=created_at.desc")
            .map { json ->
                val arr = JSONArray(json.ifBlank { "[]" })
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            RemoteVoiceRoom(
                                id = o.optString("id"),
                                title = o.optString("title"),
                                creatorId = o.optString("creator_id"),
                                isActive = o.optBoolean("is_active", true),
                                createdAt = o.optEpochMillis("created_at"),
                                description = o.optString("description"),
                                moderatorId = o.optString("moderator_id"),
                            ),
                        )
                    }
                }
            }
    }

    suspend fun createRoom(id: String, title: String, description: String = ""): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
            val body = JSONObject()
                .put("id", id)
                .put("title", title)
                .put("creator_id", rest.userId())
                .put("is_active", true)
            if (description.isNotBlank()) body.put("description", description)
            rest.post("community_voice_rooms", body, prefer = "resolution=merge-duplicates").map { }
        }

    suspend fun updateRoom(
        id: String,
        title: String? = null,
        description: String? = null,
        moderatorId: String? = null,
        clearModerator: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val body = JSONObject()
        if (title != null) body.put("title", title)
        if (description != null) body.put("description", description)
        when {
            clearModerator -> body.put("moderator_id", JSONObject.NULL)
            moderatorId != null -> body.put("moderator_id", moderatorId)
        }
        if (body.length() == 0) return@withContext Result.success(Unit)
        rest.patch("community_voice_rooms?id=eq.$id", body).map { }
    }

    suspend fun deleteRoom(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val rpc = rest.rpc("delete_community_voice_room", JSONObject().put("p_room", id))
        if (rpc.isSuccess) return@withContext rpc.map { }
        rest.patch(
            "community_voice_rooms?id=eq.$id",
            JSONObject().put("is_active", false),
        ).map { }
            .also {
                rest.delete("community_voice_rooms?id=eq.$id")
            }
    }

    suspend fun listParticipants(): Result<List<RemoteVoiceParticipant>> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(emptyList())
            rest.get("community_voice_participants?select=*").map { json ->
                val arr = JSONArray(json.ifBlank { "[]" })
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            RemoteVoiceParticipant(
                                roomId = o.optString("room_id"),
                                userId = o.optString("user_id"),
                                displayName = o.optString("display_name"),
                                muted = o.optBoolean("muted"),
                                deafened = o.optBoolean("deafened"),
                                joinedAt = o.optEpochMillis("joined_at"),
                            ),
                        )
                    }
                }
            }
        }

    suspend fun joinRoom(roomId: String, displayName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(Unit)
            val body = JSONObject()
                .put("room_id", roomId)
                .put("user_id", rest.userId())
                .put("display_name", displayName)
            rest.post("community_voice_participants", body, prefer = "resolution=merge-duplicates")
                .map { }
        }

    suspend fun updateParticipant(
        roomId: String,
        muted: Boolean? = null,
        deafened: Boolean? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val body = JSONObject()
        if (muted != null) body.put("muted", muted)
        if (deafened != null) body.put("deafened", deafened)
        if (body.length() == 0) return@withContext Result.success(Unit)
        rest.patch(
            "community_voice_participants?room_id=eq.$roomId&user_id=eq.${rest.userId()}",
            body,
        ).map { }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        rest.delete("community_voice_participants?room_id=eq.$roomId&user_id=eq.${rest.userId()}")
            .map { }
    }

    suspend fun sendSignal(sessionId: String, signal: Signal): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(Unit)
            val body = JSONObject()
                .put("session_id", sessionId)
                .put("from_user_id", rest.userId())
                .put("type", signal.type.name)
            signal.sdp?.let { body.put("sdp", it) }
            signal.candidate?.let { body.put("candidate", it) }
            signal.sdpMid?.let { body.put("sdp_mid", it) }
            signal.sdpMLineIndex?.let { body.put("sdp_mline_index", it) }
            rest.post("community_voice_signals", body).map { }
        }

    suspend fun listSignals(sessionId: String): Result<List<JSONObject>> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(emptyList())
            rest.get("community_voice_signals?select=*&session_id=eq.$sessionId&order=created_at.asc")
                .map { parseSignalRows(it) }
        }

    suspend fun listRoomSignals(roomId: String): Result<List<JSONObject>> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(emptyList())
            val encoded = java.net.URLEncoder.encode("$roomId*", Charsets.UTF_8.name())
            rest.get("community_voice_signals?select=*&session_id=like.$encoded&order=created_at.asc")
                .map { parseSignalRows(it) }
        }

    private fun parseSignalRows(json: String): List<JSONObject> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) add(arr.getJSONObject(i))
        }
    }

    suspend fun upsertCall(
        id: String,
        calleeId: String,
        callerName: String,
        calleeName: String,
        status: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
        val body = JSONObject()
            .put("id", id)
            .put("caller_id", rest.userId())
            .put("callee_id", calleeId)
            .put("caller_name", callerName)
            .put("callee_name", calleeName)
            .put("status", status)
        rest.post("community_calls", body, prefer = "resolution=merge-duplicates").map { }
    }

    suspend fun updateCallStatus(id: String, status: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(Unit)
            rest.patch("community_calls?id=eq.$id", JSONObject().put("status", status)).map { }
        }

    suspend fun listIncomingCalls(): Result<List<RemoteCall>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        val me = rest.userId()
        rest.get("community_calls?select=*&callee_id=eq.$me&status=eq.RINGING&order=created_at.desc")
            .map { json ->
                val arr = JSONArray(json.ifBlank { "[]" })
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            RemoteCall(
                                id = o.optString("id"),
                                callerId = o.optString("caller_id"),
                                calleeId = o.optString("callee_id"),
                                callerName = o.optString("caller_name"),
                                calleeName = o.optString("callee_name"),
                                status = o.optString("status"),
                                createdAt = o.optEpochMillis("created_at"),
                            ),
                        )
                    }
                }
            }
    }
}
