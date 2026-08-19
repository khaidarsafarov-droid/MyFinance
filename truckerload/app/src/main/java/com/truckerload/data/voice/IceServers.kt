package com.truckerload.data.voice

import com.truckerload.BuildConfig
import org.webrtc.PeerConnection

object IceServers {
    fun list(): List<PeerConnection.IceServer> {
        val servers = mutableListOf(
            stun("stun:stun.l.google.com:19302"),
            stun("stun:stun1.l.google.com:19302"),
            stun("stun:stun.cloudflare.com:3478"),
        )
        val turn = BuildConfig.TURN_URI.trim()
        if (turn.isNotBlank()) {
            val builder = PeerConnection.IceServer.builder(turn)
            val user = BuildConfig.TURN_USERNAME.trim()
            val cred = BuildConfig.TURN_CREDENTIAL.trim()
            if (user.isNotBlank()) builder.setUsername(user)
            if (cred.isNotBlank()) builder.setPassword(cred)
            servers += builder.createIceServer()
        }
        return servers
    }

    private fun stun(uri: String): PeerConnection.IceServer =
        PeerConnection.IceServer.builder(uri).createIceServer()
}
