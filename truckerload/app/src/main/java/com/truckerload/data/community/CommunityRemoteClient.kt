package com.truckerload.data.community

import com.truckerload.data.preferences.AuthStore
import com.truckerload.domain.social.CommunityWeekWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class CommunityRemoteClient(
    authStore: AuthStore,
) {
    private val rest = CommunityRestClient(authStore)

    fun isReady(): Boolean = rest.isReady()

    fun actorId(): String = rest.userId()

    suspend fun listPeers(window: CommunityWeekWindow = CommunityWeekWindow.current()): Result<List<RemoteCommunityPeer>> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(emptyList())
            val body = JSONObject().put("p_year", window.year).put("p_week", window.week)
            rest.rpc("list_community_peers", body).map { json ->
                val arr = JSONArray(json.ifBlank { "[]" })
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            RemoteCommunityPeer(
                                userId = o.optString("user_id"),
                                displayName = o.optString("display_name").ifBlank { "Driver" },
                                weeklyMiles = o.optDouble("weekly_miles"),
                                weeklyRevenue = o.optDouble("weekly_revenue"),
                                weeklyLoads = o.optInt("weekly_loads"),
                                weeklyRpm = o.optDouble("weekly_rpm"),
                            ),
                        )
                    }
                }.filter { it.userId.isNotBlank() }
            }
        }

    suspend fun listChats(): Result<List<RemoteCommunityChat>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.get("community_chats?select=*&order=last_message_at.desc").map { parseChats(it) }
    }

    suspend fun listMembers(): Result<List<RemoteCommunityMember>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.get("community_chat_members?select=*").map { parseMembers(it) }
    }

    suspend fun listMessages(): Result<List<RemoteCommunityMessage>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        rest.get("community_messages?select=*&order=sent_at.asc").map { parseMessages(it) }
    }

    suspend fun createDm(peerId: String): Result<String> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
        rest.rpc("create_or_get_dm", JSONObject().put("p_peer", peerId)).mapCatching { raw ->
            rest.parseUuid(raw).ifBlank { error("empty dm id") }
        }
    }

    suspend fun createGroup(title: String, category: String): Result<String> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
            rest.rpc(
                "create_community_group",
                JSONObject().put("p_title", title).put("p_category", category),
            ).mapCatching { raw -> rest.parseUuid(raw).ifBlank { error("empty group id") } }
        }

    suspend fun joinByInvite(code: String): Result<String> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
        rest.rpc("join_group_by_invite", JSONObject().put("p_code", code)).mapCatching { raw ->
            rest.parseUuid(raw).ifBlank { error("empty group id") }
        }
    }

    suspend fun joinChat(chatId: String, displayName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(Unit)
            val body = JSONObject()
                .put("chat_id", chatId)
                .put("user_id", rest.userId())
                .put("display_name", displayName.ifBlank { "You" })
                .put("role", "MEMBER")
            rest.post("community_chat_members", body, prefer = "resolution=merge-duplicates")
                .map { }
        }

    suspend fun leaveGroup(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val me = rest.userId()
        rest.delete("community_chat_members?chat_id=eq.$chatId&user_id=eq.$me").map { }
    }

    suspend fun sendMessage(
        id: String,
        chatId: String,
        senderName: String,
        text: String,
        messageType: String,
        attachmentUrl: String?,
        replyToId: String?,
        locationLabel: String?,
        durationMs: Long,
        sentAt: Long,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
        val body = JSONObject()
            .put("id", id)
            .put("chat_id", chatId)
            .put("sender_id", rest.userId())
            .put("sender_name", senderName)
            .put("body", text)
            .put("message_type", messageType)
            .put("duration_ms", durationMs)
            .put("sent_at", CommunityTime.toIso(sentAt))
        if (!attachmentUrl.isNullOrBlank()) body.put("attachment_url", attachmentUrl)
        if (!replyToId.isNullOrBlank()) body.put("reply_to_id", replyToId)
        if (!locationLabel.isNullOrBlank()) body.put("location_label", locationLabel)
        rest.post("community_messages", body, prefer = "resolution=merge-duplicates").map { }
            .also {
                rest.patch(
                    "community_chats?id=eq.$chatId",
                    JSONObject()
                        .put("last_message", text.take(240))
                        .put("last_message_at", CommunityTime.toIso(sentAt)),
                )
            }
    }

    suspend fun addReaction(messageId: String, reaction: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(Unit)
            val body = JSONObject()
                .put("message_id", messageId)
                .put("user_id", rest.userId())
                .put("reaction", reaction)
            rest.post("community_reactions", body, prefer = "resolution=merge-duplicates").map { }
        }

    suspend fun upsertWeeklyStats(
        window: CommunityWeekWindow,
        miles: Double,
        loads: Int,
        revenue: Double,
        rpm: Double,
        shareEnabled: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val body = JSONObject()
            .put("user_id", rest.userId())
            .put("iso_year", window.year)
            .put("iso_week", window.week)
            .put("miles", miles)
            .put("loads", loads)
            .put("revenue", revenue)
            .put("rpm", rpm)
            .put("share_enabled", shareEnabled)
            .put("updated_at", CommunityTime.toIso(System.currentTimeMillis()))
        rest.post(
            "community_weekly_stats",
            body,
            prefer = "resolution=merge-duplicates,on_conflict=user_id,iso_year,iso_week",
        ).map { }
    }

    suspend fun setShareWeeklyStats(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        rest.patch(
            "profiles?id=eq.${rest.userId()}",
            JSONObject().put("share_weekly_stats", enabled),
        ).map { }
    }

    suspend fun joinChallenge(challengeId: String, score: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
            val body = JSONObject()
                .put("challenge_id", challengeId)
                .put("user_id", rest.userId())
                .put("score", score)
                .put("joined_at", CommunityTime.toIso(System.currentTimeMillis()))
            rest.post(
                "community_challenge_participation",
                body,
                prefer = "resolution=merge-duplicates,on_conflict=challenge_id,user_id",
            ).map { }
        }

    suspend fun listChallengeBoard(challengeId: String): Result<List<Pair<String, Double>>> =
        withContext(Dispatchers.IO) {
            if (!rest.isReady()) return@withContext Result.success(emptyList())
            rest.get(
                "community_challenge_participation?select=user_id,score&challenge_id=eq.$challengeId&order=score.desc",
            ).map { json ->
                val arr = JSONArray(json.ifBlank { "[]" })
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(o.optString("user_id") to o.optDouble("score"))
                    }
                }
            }
        }

    suspend fun listStatuses(): Result<List<RemoteCommunityStatus>> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(emptyList())
        val expires = java.net.URLEncoder.encode(
            CommunityTime.toIso(System.currentTimeMillis()),
            "UTF-8",
        )
        rest.get("community_statuses?select=*&expires_at=gt.$expires&order=created_at.desc")
            .map { parseStatuses(it) }
    }

    suspend fun createStatus(
        id: String,
        displayName: String,
        type: String,
        text: String?,
        mediaPath: String?,
        durationMs: Long,
        createdAt: Long,
        expiresAt: Long,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.failure(IllegalStateException("community offline"))
        val body = JSONObject()
            .put("id", id)
            .put("user_id", rest.userId())
            .put("display_name", displayName)
            .put("type", type)
            .put("duration_ms", durationMs)
            .put("created_at", CommunityTime.toIso(createdAt))
            .put("expires_at", CommunityTime.toIso(expiresAt))
        if (!text.isNullOrBlank()) body.put("body", text)
        if (!mediaPath.isNullOrBlank()) body.put("media_path", mediaPath)
        rest.post("community_statuses", body, prefer = "resolution=merge-duplicates").map { }
    }

    suspend fun blockUser(blockedId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        val body = JSONObject()
            .put("blocker_id", rest.userId())
            .put("blocked_id", blockedId)
        rest.post("community_blocks", body, prefer = "resolution=merge-duplicates").map { }
    }

    suspend fun unblockUser(blockedId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!rest.isReady()) return@withContext Result.success(Unit)
        rest.delete("community_blocks?blocker_id=eq.${rest.userId()}&blocked_id=eq.$blockedId")
            .map { }
    }

    private fun parseChats(json: String): List<RemoteCommunityChat> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RemoteCommunityChat(
                        id = o.optString("id"),
                        type = o.optString("type"),
                        title = o.optString("title"),
                        inviteCode = o.optString("invite_code"),
                        creatorId = o.optString("creator_id"),
                        category = o.optString("category"),
                        isPublic = o.optBoolean("is_public"),
                        lastMessage = o.optString("last_message"),
                        lastMessageAt = o.optEpochMillis("last_message_at"),
                    ),
                )
            }
        }
    }

    private fun parseMembers(json: String): List<RemoteCommunityMember> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RemoteCommunityMember(
                        chatId = o.optString("chat_id"),
                        userId = o.optString("user_id"),
                        displayName = o.optString("display_name"),
                        role = o.optString("role").ifBlank { "MEMBER" },
                        joinedAt = o.optEpochMillis("joined_at"),
                    ),
                )
            }
        }
    }

    private fun parseMessages(json: String): List<RemoteCommunityMessage> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RemoteCommunityMessage(
                        id = o.optString("id"),
                        chatId = o.optString("chat_id"),
                        senderId = o.optString("sender_id"),
                        senderName = o.optString("sender_name"),
                        text = o.optString("body"),
                        messageType = o.optString("message_type").ifBlank { "TEXT" },
                        attachmentUrl = o.optString("attachment_url").takeIf { it.isNotBlank() },
                        replyToId = o.optString("reply_to_id").takeIf { it.isNotBlank() },
                        locationLabel = o.optString("location_label").takeIf { it.isNotBlank() },
                        durationMs = o.optLong("duration_ms"),
                        sentAt = o.optEpochMillis("sent_at"),
                    ),
                )
            }
        }
    }

    private fun parseStatuses(json: String): List<RemoteCommunityStatus> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    RemoteCommunityStatus(
                        id = o.optString("id"),
                        userId = o.optString("user_id"),
                        displayName = o.optString("display_name"),
                        type = o.optString("type").ifBlank { "TEXT" },
                        text = o.optString("body").takeIf { it.isNotBlank() },
                        mediaPath = o.optString("media_path").takeIf { it.isNotBlank() },
                        durationMs = o.optLong("duration_ms"),
                        createdAt = o.optEpochMillis("created_at"),
                        expiresAt = o.optEpochMillis("expires_at"),
                    ),
                )
            }
        }
    }
}
