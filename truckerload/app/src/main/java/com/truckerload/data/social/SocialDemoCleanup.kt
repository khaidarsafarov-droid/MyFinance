package com.truckerload.data.social

import com.truckerload.data.local.AppDatabase

/**
 * Removes hardcoded prototype community content (demo peers, groups, DMs, statuses, voice rooms)
 * that older builds seeded into Room. Idempotent — safe to run on every social init.
 */
object SocialDemoCleanup {

    val DEMO_CHAT_IDS = listOf(
        "group_i95",
        "group_fuel",
        "group_help",
        "dm_peer_alexey",
    )

    val DEMO_PEER_IDS = listOf(
        "peer_ivan",
        "peer_alexey",
        "peer_sergey",
        "peer_dmitry",
        "peer_andrey",
        "peer_maria",
    )

    val DEMO_STATUS_IDS = listOf(
        "status_peer_ivan",
        "status_peer_alexey",
        "status_peer_sergey",
        "status_me",
    )

    val DEMO_VOICE_ROOM_IDS = listOf(
        "voice_general",
        "voice_routes",
        "voice_fuel",
        "voice_lounge",
    )

    suspend fun purge(db: AppDatabase) {
        val chatDao = db.socialChatDao()
        val messageDao = db.socialMessageDao()
        val memberDao = db.chatMemberDao()
        val peerDao = db.socialPeerDao()
        val statusDao = db.driverStatusDao()
        val roomDao = db.voiceRoomDao()
        val participantDao = db.voiceRoomParticipantDao()

        DEMO_CHAT_IDS.forEach { chatId ->
            messageDao.deleteAllInChat(chatId)
            chatDao.deleteChat(chatId)
        }
        memberDao.deleteAllInChats(DEMO_CHAT_IDS)
        peerDao.deleteByIds(DEMO_PEER_IDS)
        statusDao.deleteByIds(DEMO_STATUS_IDS)
        participantDao.deleteAllInRooms(DEMO_VOICE_ROOM_IDS)
        roomDao.deleteByIds(DEMO_VOICE_ROOM_IDS)
    }
}
