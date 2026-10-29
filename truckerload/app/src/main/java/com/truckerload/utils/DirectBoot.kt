package com.truckerload.utils

import android.content.Context
import android.os.UserManager

/** Credential-encrypted storage is unavailable until the user unlocks the device. */
object DirectBoot {
    fun isUserUnlocked(context: Context): Boolean {
        if (ROBOLECTRIC) return true
        return runCatching {
            val um = context.getSystemService(UserManager::class.java) ?: return true
            um.isUserUnlocked
        }.getOrDefault(true)
    }

    private val ROBOLECTRIC: Boolean = runCatching {
        Class.forName("org.robolectric.Robolectric")
        true
    }.getOrDefault(false)
}
