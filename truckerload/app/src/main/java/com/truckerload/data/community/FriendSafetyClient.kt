package com.truckerload.data.community

import com.truckerload.data.preferences.AuthStore
import com.truckerload.domain.friends.FriendRequest
import com.truckerload.domain.friends.FriendRequestDirection
import com.truckerload.domain.friends.FriendRequestSendResult
import com.truckerload.domain.friends.FriendRequestSendResults
import com.truckerload.domain.social.CommunityReportReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class FriendSafetyClient(
    authStore: AuthStore,
) {
    private val rest = CommunityRestClient(authStore)

    fun isReady(): Boolean = rest.isReady()

    suspend fun sendFriendRequest(peerId: String): Result<FriendRequestSendResult> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) {
                return@withContext Result.failure(IllegalStateException("offline"))
            }
            rest.rpc("send_friend_request", JSONObject().put("p_peer", peerId)).mapCatching { raw ->
                FriendRequestSendResults.fromRpc(raw)
            }.recoverCatching { err ->
                if (isMissingRpc(err.message.orEmpty())) {
                    throw IllegalStateException(ERROR_SAFETY_SCHEMA_MISSING)
                }
                throw err
            }
        }

    suspend fun listFriendRequests(): Result<List<FriendRequest>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.rpc("list_my_friend_requests", JSONObject()).mapCatching { json ->
            parseRequests(json)
        }.recoverCatching { err ->
            if (isMissingRpc(err.message.orEmpty())) emptyList() else throw err
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> =
        rpcUnit("accept_friend_request", JSONObject().put("p_request", requestId))

    suspend fun declineFriendRequest(requestId: String): Result<Unit> =
        rpcUnit("decline_friend_request", JSONObject().put("p_request", requestId))

    suspend fun cancelFriendRequest(requestId: String): Result<Unit> =
        rpcUnit("cancel_friend_request", JSONObject().put("p_request", requestId))

    suspend fun submitReport(
        reportedUserId: String,
        reason: CommunityReportReason,
        details: String = "",
        chatId: String? = null,
        messageId: String? = null,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val body = JSONObject()
            .put("p_user", reportedUserId)
            .put("p_reason", reason.rpcValue)
            .put("p_details", details.take(500))
        if (!chatId.isNullOrBlank()) body.put("p_chat", chatId)
        if (!messageId.isNullOrBlank()) body.put("p_message", messageId)
        rest.rpc("submit_community_report", body).map { }.recoverCatching { err ->
            if (isMissingRpc(err.message.orEmpty())) Unit else throw err
        }
    }

    suspend fun listBlockedIds(): Result<List<String>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.get(
            "community_blocks?select=blocked_id&blocker_id=eq.${rest.userId()}",
        ).mapCatching { json ->
            val arr = JSONArray(json.ifBlank { "[]" })
            buildList {
                for (i in 0 until arr.length()) {
                    val id = arr.getJSONObject(i).optString("blocked_id")
                    if (id.isNotBlank()) add(id)
                }
            }
        }
    }

    private suspend fun rpcUnit(fn: String, body: JSONObject): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) {
                return@withContext Result.failure(IllegalStateException("offline"))
            }
            rest.rpc(fn, body).map { }.recoverCatching { err ->
                if (isMissingRpc(err.message.orEmpty())) {
                    throw IllegalStateException(ERROR_SAFETY_SCHEMA_MISSING)
                }
                throw err
            }
        }

    companion object {
        const val ERROR_SAFETY_SCHEMA_MISSING = "schema_friend_safety_missing"

        fun isMissingRpc(message: String): Boolean {
            val m = message.lowercase()
            return m.contains("pgrst202") ||
                m.contains("could not find the function") ||
                (m.contains("does not exist") && m.contains("friend_request"))
        }

        internal fun parseRequests(json: String): List<FriendRequest> {
            val arr = JSONArray(json.ifBlank { "[]" })
            return buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val direction = if (o.optString("direction") == "outgoing") {
                        FriendRequestDirection.OUTGOING
                    } else {
                        FriendRequestDirection.INCOMING
                    }
                    add(
                        FriendRequest(
                            id = o.optString("request_id"),
                            peerId = o.optString("peer_id"),
                            peerNickname = o.optString("peer_nickname").ifBlank { "Driver" },
                            direction = direction,
                            createdAtMillis = parseMillis(o.optString("created_at")),
                        ),
                    )
                }
            }
        }

        private fun parseMillis(raw: String): Long =
            runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
    }
}
