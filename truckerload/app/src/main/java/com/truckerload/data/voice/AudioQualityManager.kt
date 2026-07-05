package com.truckerload.data.voice

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import com.truckerload.domain.voice.VoiceRoomSettings

class AudioQualityManager(private val context: Context) {
    private var settings = VoiceRoomSettings()

    fun currentSettings(): VoiceRoomSettings = settings

    fun adjustForNetwork(): VoiceRoomSettings {
        val kbps = estimateDownloadKbps()
        settings = when {
            kbps >= 1000 -> VoiceRoomSettings(bitrate = 128_000, sampleRate = 48_000)
            kbps >= 500 -> VoiceRoomSettings(bitrate = 64_000, sampleRate = 44_100)
            kbps >= 100 -> VoiceRoomSettings(bitrate = 32_000, sampleRate = 32_000)
            else -> VoiceRoomSettings(bitrate = 16_000, sampleRate = 16_000)
        }
        return settings
    }

    private fun estimateDownloadKbps(): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return 100
        val caps = cm.getNetworkCapabilities(network) ?: return 100
        val down = caps.linkDownstreamBandwidthKbps
        return if (down > 0) down else when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 2_000
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 400
            else -> 150
        }
    }
}
