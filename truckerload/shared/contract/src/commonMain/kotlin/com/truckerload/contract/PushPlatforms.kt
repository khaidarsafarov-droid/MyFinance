package com.truckerload.contract

/**
 * Stable platform labels shared with the iOS façade.
 * There is no product push backend; these strings are identifiers only.
 */
object PushPlatforms {
    const val ANDROID = "android"
    const val IOS = "ios"

    val supported: Set<String> = setOf(ANDROID, IOS)

    fun isSupported(value: String): Boolean = value in supported
}
