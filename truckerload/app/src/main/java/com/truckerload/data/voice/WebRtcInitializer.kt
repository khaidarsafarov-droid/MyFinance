package com.truckerload.data.voice

import android.content.Context
import org.webrtc.PeerConnectionFactory

/**
 * Process-wide WebRTC native init. [PeerConnectionFactory.initialize] must run once;
 * factories themselves can be created and disposed per call/room.
 */
object WebRtcInitializer {
    private val lock = Any()

    @Volatile
    private var nativeReady = false

    fun ensureNative(context: Context) {
        if (nativeReady) return
        synchronized(lock) {
            if (nativeReady) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions(),
            )
            nativeReady = true
        }
    }

    fun createFactory(context: Context): PeerConnectionFactory {
        ensureNative(context)
        return PeerConnectionFactory.builder().createPeerConnectionFactory()
    }
}
