package com.truckerload.data.voice

import com.truckerload.data.community.CommunityVoiceRemote
import com.truckerload.data.local.dao.VoiceSignalDao
import com.truckerload.data.local.entities.VoiceSignalEntity
import com.truckerload.domain.voice.Signal
import com.truckerload.domain.voice.SignalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.UUID

class HybridSignalingService(
    private val signalDao: VoiceSignalDao,
    private val remote: CommunityVoiceRemote,
) : SignalingService {
    override suspend fun sendSignal(sessionId: String, signal: Signal): Result<Unit> {
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
        remote.sendSignal(sessionId, signal)
        return Result.success(Unit)
    }

    override fun watchSignals(sessionId: String, excludeUserId: String): Flow<List<Signal>> =
        signalDao.watchSignals(sessionId).map { list ->
            list.filter { it.fromUserId != excludeUserId }.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)

    override suspend fun clearSignals(sessionId: String): Result<Unit> = runCatching {
        signalDao.clear(sessionId)
    }

    suspend fun pullRemote(sessionId: String) {
        val rows = remote.listSignals(sessionId).getOrElse { return }
        rows.forEach { o ->
            val id = o.optString("id").ifBlank { UUID.randomUUID().toString() }
            signalDao.insert(
                VoiceSignalEntity(
                    id = id,
                    sessionId = o.optString("session_id"),
                    fromUserId = o.optString("from_user_id"),
                    type = o.optString("type"),
                    sdp = o.optString("sdp").takeIf { it.isNotBlank() },
                    candidate = o.optString("candidate").takeIf { it.isNotBlank() },
                    sdpMid = o.optString("sdp_mid").takeIf { it.isNotBlank() },
                    sdpMLineIndex = if (o.has("sdp_mline_index")) o.optInt("sdp_mline_index") else null,
                    timestamp = com.truckerload.data.community.CommunityTime.parseMillis(
                        o.optString(
                            "created_at"
                        )
                    ),
                ),
            )
        }
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
