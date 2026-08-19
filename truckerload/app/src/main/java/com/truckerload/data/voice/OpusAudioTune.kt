package com.truckerload.data.voice

import com.truckerload.domain.voice.VoiceAudioBudget
import org.webrtc.PeerConnection

/**
 * Pins Opus to a speech-mode, DTX-friendly profile so 1:1 P2P and mesh
 * stay inside the highway budget (≤20 kbps).
 */
object OpusAudioTune {
    fun constrainSdp(sdp: String, bitrateBps: Int = VoiceAudioBudget.HIGHWAY_BPS): String {
        val bps = bitrateBps.coerceIn(VoiceAudioBudget.TELEPHONE_BPS, VoiceAudioBudget.SPEECH_BPS)
        val opusPt = OPUS_PT.find(sdp)?.groupValues?.get(1) ?: "111"
        val extras = listOf(
            "useinbandfec=1",
            "usedtx=1",
            "stereo=0",
            "sprop-stereo=0",
            "maxaveragebitrate=$bps",
            "maxplaybackrate=16000",
            "ptime=20",
            "minptime=10",
        )
        var foundFmtp = false
        val lines = sdp.splitToSequence("\r\n", "\n").map { line ->
            if (line.startsWith("a=fmtp:$opusPt")) {
                foundFmtp = true
                mergeFmtp(line, extras)
            } else {
                line
            }
        }.toMutableList()
        if (!foundFmtp) {
            val rtpmapIndex = lines.indexOfFirst { it.startsWith("a=rtpmap:$opusPt") }
            if (rtpmapIndex >= 0) {
                lines.add(rtpmapIndex + 1, "a=fmtp:$opusPt ${extras.joinToString(";")}")
            }
        }
        return lines.joinToString("\r\n")
    }

    fun capSenderBitrate(peerConnection: PeerConnection?, bitrateBps: Int = VoiceAudioBudget.HIGHWAY_BPS) {
        val bps = bitrateBps.coerceIn(VoiceAudioBudget.TELEPHONE_BPS, VoiceAudioBudget.SPEECH_BPS)
        peerConnection?.senders.orEmpty().forEach { sender ->
            val params = sender.parameters ?: return@forEach
            params.encodings.orEmpty().forEach { encoding ->
                encoding.maxBitrateBps = bps
            }
            runCatching { sender.parameters = params }
        }
    }

    private fun mergeFmtp(line: String, extras: List<String>): String {
        val prefix = line.substringBefore(':') + ":" + line.substringAfter(':').substringBefore(' ')
        val existing = line.substringAfter(' ', missingDelimiterValue = "")
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val keys = existing.map { it.substringBefore('=') }.toMutableSet()
        val merged = existing.toMutableList()
        extras.forEach { extra ->
            val key = extra.substringBefore('=')
            if (key !in keys) {
                merged += extra
                keys += key
            }
        }
        return "$prefix ${merged.joinToString(";")}"
    }

    private val OPUS_PT = Regex("""a=rtpmap:(\d+) opus/""", RegexOption.IGNORE_CASE)
}
