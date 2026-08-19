package com.truckerload.data.voice

/**
 * Deterministic per-pair signaling session for a voice-room mesh.
 * Both peers use the same session id so offers/answers/ICE meet in one channel.
 */
object VoiceMeshSession {
    private const val SEP = "__"

    fun pairId(roomId: String, userA: String, userB: String): String {
        val (left, right) = if (userA <= userB) userA to userB else userB to userA
        return "$roomId$SEP$left$SEP$right"
    }

    /** Lexicographically smaller user id creates the SDP offer (avoids glare). */
    fun isOfferer(localUserId: String, remoteUserId: String): Boolean =
        localUserId < remoteUserId
}
