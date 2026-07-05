package com.truckerload.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truckerload.data.local.entities.CallSessionEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.data.local.entities.VoiceSignalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceRoomDao {
    @Query("SELECT * FROM voice_rooms WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun watchActiveRooms(): Flow<List<VoiceRoomEntity>>

    @Query("SELECT * FROM voice_rooms WHERE id = :roomId LIMIT 1")
    suspend fun getRoom(roomId: String): VoiceRoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(room: VoiceRoomEntity)

    @Query("SELECT COUNT(*) FROM voice_rooms")
    suspend fun count(): Int
}

@Dao
interface VoiceRoomParticipantDao {
    @Query("SELECT * FROM voice_room_participants WHERE roomId = :roomId ORDER BY joinedAt ASC")
    fun watchParticipants(roomId: String): Flow<List<VoiceRoomParticipantEntity>>

    @Query("SELECT * FROM voice_room_participants")
    fun watchAllParticipants(): Flow<List<VoiceRoomParticipantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(participants: List<VoiceRoomParticipantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(participant: VoiceRoomParticipantEntity)

    @Query("DELETE FROM voice_room_participants WHERE roomId = :roomId AND userId = :userId")
    suspend fun remove(roomId: String, userId: String)

    @Query("UPDATE voice_room_participants SET isMuted = :muted WHERE roomId = :roomId AND userId = :userId")
    suspend fun setMuted(roomId: String, userId: String, muted: Boolean)

    @Query("UPDATE voice_room_participants SET isDeafened = :deafened WHERE roomId = :roomId AND userId = :userId")
    suspend fun setDeafened(roomId: String, userId: String, deafened: Boolean)

    @Query("UPDATE voice_room_participants SET isSpeaking = :speaking, audioLevel = :level WHERE roomId = :roomId AND userId = :userId")
    suspend fun setSpeaking(roomId: String, userId: String, speaking: Boolean, level: Int)
}

@Dao
interface CallSessionDao {
    @Query("SELECT * FROM call_sessions WHERE status = 'RINGING' AND isIncoming = 1 ORDER BY startedAt DESC LIMIT 1")
    fun watchIncomingCall(): Flow<CallSessionEntity?>

    @Query("SELECT * FROM call_sessions WHERE callId = :callId LIMIT 1")
    suspend fun getCall(callId: String): CallSessionEntity?

    @Query("SELECT * FROM call_sessions WHERE callId = :callId LIMIT 1")
    fun watchCall(callId: String): Flow<CallSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(call: CallSessionEntity)

    @Query("UPDATE call_sessions SET status = :status, endedAt = :endedAt, durationMs = :durationMs WHERE callId = :callId")
    suspend fun updateStatus(callId: String, status: String, endedAt: Long?, durationMs: Long)
}

@Dao
interface VoiceSignalDao {
    @Query("SELECT * FROM voice_signals WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun watchSignals(sessionId: String): Flow<List<VoiceSignalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(signal: VoiceSignalEntity)

    @Query("DELETE FROM voice_signals WHERE sessionId = :sessionId")
    suspend fun clear(sessionId: String)
}
