package com.truckerload.data.voice

import com.truckerload.data.local.dao.VoiceSignalDao
import com.truckerload.data.local.entities.VoiceSignalEntity
import com.truckerload.domain.voice.Signal
import com.truckerload.domain.voice.SignalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface SignalingService {
    suspend fun sendSignal(sessionId: String, signal: Signal): Result<Unit>
    fun watchSignals(sessionId: String, excludeUserId: String): Flow<List<Signal>>
    suspend fun clearSignals(sessionId: String): Result<Unit>
}

class LocalSignalingService(
    private val signalDao: VoiceSignalDao,
) : SignalingService {
    override suspend fun sendSignal(sessionId: String, signal: Signal): Result<Unit> = runCatching {
        signalDao.insert(
            VoiceSignalEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                fromUserId = signal.fromUserId,
                type = signal.type.name,
                sdp = signal.sdp,
                candidate = signal.candidate,
                sdpMid = signal.sdpMid,
                sdpMLineIndex = signal.sdpMLineIndex,
                timestamp = signal.timestamp,
            ),
        )
    }

    override fun watchSignals(sessionId: String, excludeUserId: String): Flow<List<Signal>> =
        signalDao.watchSignals(sessionId).map { list ->
            list.filter { it.fromUserId != excludeUserId }.map { it.toDomain() }
        }

    override suspend fun clearSignals(sessionId: String): Result<Unit> = runCatching {
        signalDao.clear(sessionId)
    }

    private fun VoiceSignalEntity.toDomain() = Signal(
        type = runCatching { SignalType.valueOf(type) }.getOrDefault(SignalType.LEAVE),
        fromUserId = fromUserId,
        sdp = sdp,
        candidate = candidate,
        sdpMid = sdpMid,
        sdpMLineIndex = sdpMLineIndex,
        timestamp = timestamp,
    )
}
