package com.truckerload.contract

/**
 * Wire values for [DevicePushTokenRequest.platform].
 *
 * APNs delivery is not implemented yet — iOS tokens may be stored so a later
 * Apple client can register, but sync wake-ups currently go only to Android/FCM.
 */
object PushPlatforms {
    const val ANDROID = "android"
    const val IOS = "ios"

    val supported: Set<String> = setOf(ANDROID, IOS)

    fun isSupported(value: String): Boolean = value in supported
}
