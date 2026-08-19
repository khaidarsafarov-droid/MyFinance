package com.truckerload.data.repository

import android.content.Context
import com.truckerload.data.community.CommunityVoiceRemote
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.CallSessionEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.data.social.SocialDemoCleanup
import com.truckerload.data.voice.AudioQualityManager
import com.truckerload.data.voice.HybridSignalingService
import com.truckerload.data.voice.SignalingService
import com.truckerload.data.voice.WebRtcAudioEngine
import com.truckerload.data.voice.WebRtcCallManager
import com.truckerload.domain.voice.CallState
import com.truckerload.domain.voice.CallStatus
import com.truckerload.domain.voice.CallType
import com.truckerload.domain.voice.Signal
import com.truckerload.domain.voice.SignalType
import com.truckerload.domain.voice.VoiceParticipant
import com.truckerload.domain.voice.VoiceRoom
import com.truckerload.domain.voice.VoiceRoomType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

class VoiceRepository(
    db: AppDatabase,
    context: Context,
    private val voiceRemote: CommunityVoiceRemote,
    private val actorId: () -> String,
) {
    private val roomDao = db.voiceRoomDao()
    private val participantDao = db.voiceRoomParticipantDao()
    private val callDao = db.callSessionDao()
    val audioEngine = WebRtcAudioEngine(context)
    val qualityManager = AudioQualityManager(context)
    val signaling: SignalingService = HybridSignalingService(db.voiceSignalDao(), voiceRemote)
    private val hybridSignals = signaling as HybridSignalingService
    private val callManager = WebRtcCallManager(context, signaling, actorId())

    private var activeRoomId: String? = null
    private var activeCallId: String? = null

    suspend fun ensureInitialized() {
        participantDao.deleteAllInRooms(SocialDemoCleanup.DEMO_VOICE_ROOM_IDS)
        roomDao.deleteByIds(SocialDemoCleanup.DEMO_VOICE_ROOM_IDS)
        pullRemote()
    }

    suspend fun pullRemote() {
        if (!voiceRemote.isReady()) return
        voiceRemote.listRooms().getOrNull().orEmpty().forEach { room ->
            roomDao.upsert(
                VoiceRoomEntity(
                    id = room.id,
                    name = room.title,
                    type = "PUBLIC",
                    creatorId = room.creatorId,
                    maxParticipants = 50,
                    isActive = room.isActive,
                    createdAt = room.createdAt,
                    updatedAt = room.createdAt,
                ),
            )
        }
        val parts = voiceRemote.listParticipants().getOrNull().orEmpty()
        if (parts.isNotEmpty()) {
            participantDao.upsertAll(
                parts.map { p ->
                    VoiceRoomParticipantEntity(
                        roomId = p.roomId,
                        userId = p.userId,
                        displayName = p.displayName,
                        isMuted = p.muted,
                        isDeafened = p.deafened,
                        isSpeaking = false,
                        audioLevel = 0,
                        joinedAt = p.joinedAt,
                    )
                },
            )
        }
        voiceRemote.listIncomingCalls().getOrNull().orEmpty().forEach { call ->
            callDao.upsert(
                CallSessionEntity(
                    callId = call.id,
                    type = CallType.P2P.name,
                    status = call.status,
                    callerId = call.callerId,
                    callerName = call.callerName,
                    calleeId = call.calleeId,
                    calleeName = call.calleeName,
                    isIncoming = call.calleeId == actorId(),
                    startedAt = call.createdAt,
                    endedAt = null,
                    durationMs = 0,
                ),
            )
        }
        activeRoomId?.let { hybridSignals.pullRemote(it) }
        activeCallId?.let { hybridSignals.pullRemote(it) }
    }

    fun watchRooms(): Flow<List<VoiceRoom>> =
        combine(
            roomDao.watchActiveRooms(),
            participantDao.watchAllParticipants(),
        ) { rooms, allParticipants ->
            rooms.map { room ->
                val participants = allParticipants
                    .filter { it.roomId == room.id }
                    .map { it.toDomain(it.userId == actorId()) }
                room.toDomain(participants)
            }
        }.flowOn(Dispatchers.IO)

    fun watchRoom(roomId: String, myName: String): Flow<VoiceRoom?> =
        combine(
            roomDao.watchActiveRooms().map { list -> list.firstOrNull { it.id == roomId } },
            participantDao.watchParticipants(roomId),
        ) { room, participants ->
            room?.toDomain(
                participants.map { it.toDomain(it.userId == actorId()) },
            )?.let { voiceRoom ->
                val me = voiceRoom.participants.find { it.isMe }
                    ?: VoiceParticipant(
                        userId = actorId(),
                        displayName = myName.ifBlank { "You" },
                        joinedAt = System.currentTimeMillis(),
                        isMe = true,
                    )
                if (voiceRoom.participants.none { it.isMe }) {
                    voiceRoom.copy(participants = voiceRoom.participants + me)
                } else voiceRoom
            }
        }.flowOn(Dispatchers.IO)

    suspend fun createRoom(name: String, type: VoiceRoomType = VoiceRoomType.PUBLIC): Result<String> = runCatching {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        roomDao.upsert(
            VoiceRoomEntity(
                id = id,
                name = name.ifBlank { "New room" },
                type = type.name,
                creatorId = actorId(),
                maxParticipants = 50,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        if (voiceRemote.isReady()) {
            voiceRemote.createRoom(id, name.ifBlank { "New room" })
        }
        id
    }

    suspend fun joinRoom(roomId: String, displayName: String): Result<Unit> = runCatching {
        activeRoomId = roomId
        val settings = qualityManager.adjustForNetwork()
        audioEngine.initialize().getOrThrow()
        audioEngine.startLocalAudio(settings).getOrThrow()
        participantDao.upsert(
            VoiceRoomParticipantEntity(
                roomId = roomId,
                userId = actorId(),
                displayName = displayName.ifBlank { "You" },
                isMuted = false,
                isDeafened = false,
                isSpeaking = false,
                audioLevel = 0,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        roomDao.getRoom(roomId)?.let { room ->
            roomDao.upsert(room.copy(updatedAt = System.currentTimeMillis()))
        }
        if (voiceRemote.isReady()) {
            voiceRemote.joinRoom(roomId, displayName)
        }
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
        participantDao.remove(roomId, actorId())
        signaling.sendSignal(
            roomId,
            Signal(type = SignalType.LEAVE, fromUserId = actorId()),
        )
        if (activeRoomId == roomId) {
            audioEngine.stopLocalAudio()
            activeRoomId = null
        }
        if (voiceRemote.isReady()) {
            voiceRemote.leaveRoom(roomId)
        }
    }

    suspend fun setMuted(roomId: String, muted: Boolean) {
        audioEngine.setMuted(muted)
        participantDao.setMuted(roomId, actorId(), muted)
        signaling.sendSignal(
            roomId,
            Signal(
                type = if (muted) SignalType.MUTE else SignalType.UNMUTE,
                fromUserId = actorId(),
            ),
        )
    }

    suspend fun setDeafened(roomId: String, deafened: Boolean) {
        audioEngine.setVolumeLevel(if (deafened) 0f else 1f)
        participantDao.setDeafened(roomId, actorId(), deafened)
    }

    suspend fun updateSpeakingLevel(roomId: String) {
        val level = audioEngine.localAudioLevel()
        participantDao.setSpeaking(
            roomId,
            actorId(),
            speaking = level > 20,
            level = level,
        )
    }

    fun watchIncomingCall(): Flow<CallState?> =
        callDao.watchIncomingCall().map { it?.toDomain() }.flowOn(Dispatchers.IO)

    fun watchCall(callId: String): Flow<CallState?> =
        callDao.watchCall(callId).map { it?.toDomain() }.flowOn(Dispatchers.IO)

    suspend fun startCall(calleeId: String, calleeName: String, callerName: String): Result<CallState> = runCatching {
        val callId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val state = CallSessionEntity(
            callId = callId,
            type = CallType.P2P.name,
            status = CallStatus.RINGING.name,
            callerId = actorId(),
            callerName = callerName,
            calleeId = calleeId,
            calleeName = calleeName,
            isIncoming = false,
            startedAt = now,
            endedAt = null,
            durationMs = 0,
        )
        callDao.upsert(state)
        val settings = qualityManager.adjustForNetwork()
        audioEngine.initialize().getOrThrow()
        audioEngine.startLocalAudio(settings).getOrThrow()
        signaling.sendSignal(
            callId,
            Signal(type = SignalType.OFFER, fromUserId = actorId(), sdp = "pending"),
        )
        if (voiceRemote.isReady()) {
            voiceRemote.upsertCall(
                callId,
                calleeId,
                callerName,
                calleeName,
                CallStatus.RINGING.name
            )
        }
        activeCallId = callId
        state.toDomain()
    }

    fun beginCallAudio(scope: CoroutineScope, callId: String, isCaller: Boolean) {
        activeCallId = callId
        if (isCaller) {
            callManager.startAsCaller(scope, callId)
        } else {
            callManager.startAsCallee(scope, callId)
        }
    }

    suspend fun acceptCall(callId: String): Result<CallState> = runCatching {
        val existing = callDao.getCall(callId) ?: error("Call not found")
        val now = System.currentTimeMillis()
        val settings = qualityManager.adjustForNetwork()
        audioEngine.initialize().getOrThrow()
        audioEngine.startLocalAudio(settings).getOrThrow()
        signaling.sendSignal(
            callId,
            Signal(type = SignalType.ANSWER, fromUserId = actorId(), sdp = "pending")
        )
        if (voiceRemote.isReady()) {
            voiceRemote.updateCallStatus(callId, CallStatus.ACTIVE.name)
        }
        callDao.updateStatus(callId, CallStatus.ACTIVE.name, null, 0)
        existing.copy(status = CallStatus.ACTIVE.name, startedAt = now).toDomain()
    }

    suspend fun rejectCall(callId: String): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        callDao.updateStatus(callId, CallStatus.REJECTED.name, now, 0)
        signaling.sendSignal(callId, Signal(type = SignalType.LEAVE, fromUserId = actorId()))
        if (voiceRemote.isReady()) {
            voiceRemote.updateCallStatus(callId, CallStatus.REJECTED.name)
        }
        if (activeCallId == callId) activeCallId = null
    }

    suspend fun endCall(callId: String): Result<Unit> = runCatching {
        val existing = callDao.getCall(callId)
        val now = System.currentTimeMillis()
        val duration = existing?.let { now - it.startedAt } ?: 0L
        callDao.updateStatus(callId, CallStatus.ENDED.name, now, duration)
        signaling.sendSignal(callId, Signal(type = SignalType.LEAVE, fromUserId = actorId()))
        signaling.clearSignals(callId)
        if (voiceRemote.isReady()) {
            voiceRemote.updateCallStatus(callId, CallStatus.ENDED.name)
        }
        callManager.release()
        audioEngine.stopLocalAudio()
        if (activeCallId == callId) activeCallId = null
    }

    fun setCallMuted(muted: Boolean) {
        callManager.setMuted(muted)
        audioEngine.setMuted(muted)
    }

    fun release() {
        callManager.release()
        audioEngine.release()
        activeRoomId = null
    }

    private fun VoiceRoomEntity.toDomain(participants: List<VoiceParticipant>) = VoiceRoom(
        id = id,
        name = name,
        type = runCatching { VoiceRoomType.valueOf(type) }.getOrDefault(VoiceRoomType.PUBLIC),
        creatorId = creatorId,
        participants = participants,
        maxParticipants = maxParticipants,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun VoiceRoomParticipantEntity.toDomain(isMe: Boolean) = VoiceParticipant(
        userId = userId,
        displayName = displayName,
        isMuted = isMuted,
        isDeafened = isDeafened,
        isSpeaking = isSpeaking,
        audioLevel = audioLevel,
        joinedAt = joinedAt,
        isMe = isMe,
    )

    private fun CallSessionEntity.toDomain() = CallState(
        callId = callId,
        type = runCatching { CallType.valueOf(type) }.getOrDefault(CallType.P2P),
        status = runCatching { CallStatus.valueOf(status) }.getOrDefault(CallStatus.ENDED),
        participants = listOfNotNull(callerId, calleeId),
        startedAt = startedAt,
        durationMs = durationMs,
        isIncoming = isIncoming,
        callerId = callerId,
        callerName = callerName,
        calleeId = calleeId,
        calleeName = calleeName,
    )
}
