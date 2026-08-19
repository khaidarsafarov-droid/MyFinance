package com.truckerload.data.voice

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Context
import com.truckerload.domain.voice.VoiceAudioBudget
import com.truckerload.domain.voice.VoiceRoomSettings

class AudioQualityManager(private val context: Context) {
    private var settings = VoiceRoomSettings()

    fun currentSettings(): VoiceRoomSettings = settings

    fun hasNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun adjustForNetwork(): VoiceRoomSettings {
        val kbps = estimateDownloadKbps()
        settings = VoiceRoomSettings(
            bitrate = VoiceAudioBudget.bitrateForEstimatedKbps(kbps),
            sampleRate = 16_000,
        )
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
