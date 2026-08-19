package com.truckerload.data.voice

import org.junit.Assert.assertTrue
import org.junit.Test

class OpusAudioTuneTest {
    @Test
    fun constrainSdp_addsSpeechOpusFmtp() {
        val sdp = """
            v=0
            m=audio 9 UDP/TLS/RTP/SAVPF 111
            a=rtpmap:111 opus/48000/2
            a=fmtp:111 minptime=10
        """.trimIndent()

        val tuned = OpusAudioTune.constrainSdp(sdp, bitrateBps = 16_000)

        assertTrue(tuned.contains("maxaveragebitrate=16000"))
        assertTrue(tuned.contains("usedtx=1"))
        assertTrue(tuned.contains("useinbandfec=1"))
        assertTrue(tuned.contains("stereo=0"))
    }

    @Test
    fun constrainSdp_insertsFmtpWhenMissing() {
        val sdp = """
            v=0
            a=rtpmap:111 opus/48000/2
            a=sendrecv
        """.trimIndent()

        val tuned = OpusAudioTune.constrainSdp(sdp, bitrateBps = 20_000)

        assertTrue(tuned.contains("a=fmtp:111"))
        assertTrue(tuned.contains("maxaveragebitrate=20000"))
    }
}
