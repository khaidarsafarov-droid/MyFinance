package com.truckerload.data.voice

import com.truckerload.data.local.dao.VoiceRoomDao
import com.truckerload.data.local.dao.VoiceRoomParticipantDao
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.domain.voice.VoiceRoomType
import java.util.UUID

object VoiceSeedData {
    suspend fun seedIfEmpty(roomDao: VoiceRoomDao, participantDao: VoiceRoomParticipantDao) {
        if (roomDao.count() > 0) return
        val now = System.currentTimeMillis()
        val rooms = listOf(
            demoRoom("voice_general", "Общий зал", VoiceRoomType.PUBLIC, now, 12),
            demoRoom("voice_routes", "Комната маршрутов", VoiceRoomType.GROUP, now, 5),
            demoRoom("voice_fuel", "Топливо и цены", VoiceRoomType.PUBLIC, now, 3),
            demoRoom("voice_lounge", "Кабинет дальнобойщиков", VoiceRoomType.PRIVATE, now, 8),
        )
        rooms.forEach { roomDao.upsert(it) }
        participantDao.upsertAll(
            listOf(
                demoParticipant("voice_general", "anton", "Антон", now - 600_000, speaking = true),
                demoParticipant("voice_general", "sergey", "Сергей", now - 500_000),
                demoParticipant("voice_general", "ivan", "Иван", now - 400_000, muted = true),
                demoParticipant("voice_routes", "dmitry", "Дмитрий", now - 300_000),
                demoParticipant("voice_routes", "alexey", "Алексей", now - 200_000, speaking = true),
                demoParticipant("voice_fuel", "maxim", "Максим", now - 100_000),
                demoParticipant("voice_lounge", "nikolay", "Николай", now - 50_000),
            ),
        )
    }

    private fun demoRoom(
        id: String,
        name: String,
        type: VoiceRoomType,
        now: Long,
        max: Int,
    ) = VoiceRoomEntity(
        id = id,
        name = name,
        type = type.name,
        creatorId = "system",
        maxParticipants = max,
        isActive = true,
        createdAt = now,
        updatedAt = now,
    )

    private fun demoParticipant(
        roomId: String,
        userId: String,
        name: String,
        joinedAt: Long,
        muted: Boolean = false,
        speaking: Boolean = false,
    ) = VoiceRoomParticipantEntity(
        roomId = roomId,
        userId = userId,
        displayName = name,
        isMuted = muted,
        isDeafened = false,
        isSpeaking = speaking,
        audioLevel = if (speaking) 70 else 10,
        joinedAt = joinedAt,
    )
}
