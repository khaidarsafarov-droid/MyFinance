package com.truckerload.data.sync

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import com.truckerload.contract.DeviceSlotPolicy
import java.util.UUID

class DeviceIdentity(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun id(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = stableAndroidId() ?: UUID.randomUUID().toString()
        prefs.edit { putString(KEY_DEVICE_ID, generated) }
        return generated
    }

    /**
     * Phone vs tablet for the account device slot. Stored on first read so rotation
     * or split-screen width changes cannot flip the slot.
     */
    fun formFactor(): String {
        prefs.getString(KEY_FORM_FACTOR, null)?.let { stored ->
            DeviceSlotPolicy.normalize(stored)?.let { return it }
        }
        val factor = DeviceSlotPolicy.fromSmallestWidthDp(
            app.resources.configuration.smallestScreenWidthDp,
        )
        prefs.edit { putString(KEY_FORM_FACTOR, factor) }
        return factor
    }

    private fun stableAndroidId(): String? {
        val androidId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()
            .orEmpty()
        if (androidId.isBlank() || androidId in BROKEN_ANDROID_IDS) return null
        if (androidId.length !in 1..128 || androidId.any { it.isISOControl() }) return null
        return androidId
    }

    companion object {
        private const val PREFS_NAME = "truckerload_device_identity"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FORM_FACTOR = "form_factor"

        /** Known-buggy ANDROID_ID on some old devices; treat as missing. */
        private val BROKEN_ANDROID_IDS = setOf("9774d56d682e549c")
    }
}
