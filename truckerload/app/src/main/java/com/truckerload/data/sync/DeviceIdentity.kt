package com.truckerload.data.sync

import android.content.Context
import java.util.UUID

class DeviceIdentity(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun id(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    companion object {
        private const val PREFS_NAME = "truckerload_device_identity"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
