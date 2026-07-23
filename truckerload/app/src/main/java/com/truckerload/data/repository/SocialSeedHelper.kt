package com.truckerload.data.repository

import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.DriverStatusDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.domain.social.StatusType
import kotlinx.coroutines.flow.first

internal class SocialSeedHelper(
    private val chatDao: SocialChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val driverStatusDao: DriverStatusDao,
) {
    suspend fun seedDemoStatuses(displayName: String, statusTtlMs: Long) {
        val now = System.currentTimeMillis()
        val existing = driverStatusDao.watchActiveStatuses(now).first()
        if (existing.any { it.userId != DriverProfileEntity.LOCAL_USER_ID }) return
        listOf(
            Triple("peer_ivan", "Ivan P.", "On I-95, great RPM!"),
            Triple("peer_alexey", "Alex S.", "Looking for load TX → FL"),
            Triple("peer_sergey", "Sergey K.", "Resting in Atlanta"),
        ).forEach { (userId, name, text) ->
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = "status_$userId",
                    userId = userId,
                    displayName = name,
                    type = StatusType.TEXT.name,
                    text = text,
                    mediaPath = null,
                    createdAt = now - 60_000,
                    expiresAt = now + statusTtlMs,
                ),
            )
        }
        if (displayName.isNotBlank()) {
            driverStatusDao.insert(
                DriverStatusEntity(
                    id = "status_me",
                    userId = DriverProfileEntity.LOCAL_USER_ID,
                    displayName = displayName,
                    type = StatusType.TEXT.name,
                    text = "On the air!",
                    mediaPath = null,
                    createdAt = now,
                    expiresAt = now + statusTtlMs,
                ),
            )
        }
    }

    suspend fun seedGroupMemberships(displayName: String) {
        val seedGroupIds = listOf("group_i95", "group_fuel", "group_help")
        val now = System.currentTimeMillis()
        seedGroupIds.forEach { groupId ->
            if (!chatMemberDao.isMember(groupId, DriverProfileEntity.LOCAL_USER_ID)) {
                chatMemberDao.upsert(
                    ChatMemberEntity(
                        chatId = groupId,
                        userId = DriverProfileEntity.LOCAL_USER_ID,
                        displayName = displayName.ifBlank { "You" },
                        role = "MEMBER",
                        joinedAt = now,
                    ),
                )
            }
        }
    }

    suspend fun backfillGroupInviteCodes() {
        val codes = mapOf(
            "group_i95" to "I95ROAD",
            "group_fuel" to "FUELNOW",
            "group_help" to "ROADHELP",
        )
        codes.forEach { (groupId, code) ->
            val chat = chatDao.getChat(groupId) ?: return@forEach
            if (chat.inviteCode.isBlank()) {
                chatDao.upsert(chat.copy(inviteCode = code))
            }
        }
    }
}
