package com.truckerload.data.social

import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.domain.social.ChatType
import java.util.UUID

object SocialSeedData {

    private val PLACEHOLDER_NAMES = setOf("", "Водитель", "Driver", "User")

    suspend fun seedIfEmpty(
        chatDao: com.truckerload.data.local.dao.SocialChatDao,
        messageDao: com.truckerload.data.local.dao.SocialMessageDao,
        profileDao: com.truckerload.data.local.dao.DriverProfileDao,
        defaultDisplayName: String,
        defaultAvatarUrl: String? = null,
        defaultPhone: String? = null,
    ) {
        val existing = profileDao.getProfile()
        if (existing == null) {
            // Minimal personal profile — no fake truck/state/about pretending the user finished setup.
            profileDao.upsert(
                DriverProfileEntity(
                    displayName = defaultDisplayName.takeIf { it !in PLACEHOLDER_NAMES }.orEmpty(),
                    avatarUrl = defaultAvatarUrl?.takeIf { it.isNotBlank() },
                    phoneNumber = defaultPhone?.takeIf { it.isNotBlank() },
                    truckType = "",
                    experienceYears = 0,
                    homeState = "",
                    routesJson = "",
                    about = "",
                    status = "ONLINE",
                ),
            )
        } else {
            profileDao.upsert(
                existing.copy(
                    displayName = when {
                        existing.displayName !in PLACEHOLDER_NAMES -> existing.displayName
                        defaultDisplayName !in PLACEHOLDER_NAMES -> defaultDisplayName
                        else -> existing.displayName
                    },
                    avatarUrl = existing.avatarUrl?.takeIf { it.isNotBlank() }
                        ?: defaultAvatarUrl?.takeIf { it.isNotBlank() },
                    phoneNumber = existing.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: defaultPhone?.takeIf { it.isNotBlank() },
                ),
            )
        }
        if (chatDao.count() > 0) return

        val now = System.currentTimeMillis()
        val chats = listOf(
            SocialChatEntity(
                id = "group_i95",
                title = "Маршрут I-95",
                type = ChatType.GROUP.name,
                participantCount = 128,
                lastMessage = "Коллеги, там авария на 45-м съезде",
                lastMessageAt = now - 12 * 60_000,
                unreadCount = 3,
                avatarEmoji = "🗺️",
                onlineCount = 12,
                category = "Маршруты",
                description = "Активные маршруты по I-95",
                rating = 4.8,
                creatorId = "peer_ivan",
                inviteCode = "I95ROAD",
            ),
            SocialChatEntity(
                id = "group_fuel",
                title = "Топливо и цены",
                type = ChatType.GROUP.name,
                participantCount = 56,
                lastMessage = "Diesel \$3.89 на TA в SC",
                lastMessageAt = now - 45 * 60_000,
                unreadCount = 0,
                avatarEmoji = "⛽",
                onlineCount = 8,
                category = "Топливо",
                description = "Цены на дизель обновляются каждый час",
                rating = 4.7,
                creatorId = "peer_alexey",
                inviteCode = "FUELNOW",
            ),
            SocialChatEntity(
                id = "group_help",
                title = "Помощь на дороге",
                type = ChatType.GROUP.name,
                participantCount = 67,
                lastMessage = "Кто рядом с Atlanta? Нужен jump start",
                lastMessageAt = now - 90 * 60_000,
                unreadCount = 1,
                avatarEmoji = "🆘",
                onlineCount = 5,
                category = "Помощь",
                description = "Круглосуточная поддержка на дороге",
                rating = 4.9,
                creatorId = "peer_sergey",
                inviteCode = "ROADHELP",
            ),
            SocialChatEntity(
                id = "dm_peer_alexey",
                title = "Алексей С.",
                type = ChatType.PRIVATE.name,
                participantCount = 2,
                lastMessage = "Привет! Ты где сейчас?",
                lastMessageAt = now - 3 * 60 * 60_000,
                unreadCount = 0,
                avatarEmoji = "👤",
                onlineCount = 1,
            ),
        )
        chatDao.upsertAll(chats)

        messageDao.insertAll(
            listOf(
                SocialMessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = "group_i95",
                    senderId = "anton",
                    senderName = "Антон",
                    text = "Привет всем! Кто сегодня на I-95?",
                    sentAt = now - 60 * 60_000,
                ),
                SocialMessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = "group_i95",
                    senderId = "sergey",
                    senderName = "Сергей",
                    text = "Я проехал Richmond — пробок нет.",
                    sentAt = now - 50 * 60_000,
                ),
                SocialMessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = "group_i95",
                    senderId = "ivan",
                    senderName = "Иван",
                    text = "Коллеги, там авария на 45-м съезде #I95 #важное",
                    sentAt = now - 12 * 60_000,
                    locationLabel = "Richmond, VA",
                ),
                SocialMessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = "dm_peer_alexey",
                    senderId = "alexey",
                    senderName = "Алексей",
                    text = "Привет! Ты где сейчас?",
                    sentAt = now - 3 * 60 * 60_000,
                ),
            ),
        )
    }
}
