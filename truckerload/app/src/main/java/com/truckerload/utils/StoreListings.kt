package com.truckerload.utils

/**
 * Store listing URLs for “Share the app”.
 *
 * Google Play URL is derived from the Android package and is valid as soon as
 * the Play Console listing is published.
 *
 * [APP_STORE_ID] stays empty until the Apple Developer / App Store Connect
 * listing exists. Filling it automatically adds the iPhone link to the share
 * text — no other UI change needed.
 */
object StoreListings {
    const val ANDROID_PACKAGE = "com.truckerload"

    /** Numeric App Store Connect ID. Blank until the iOS listing is live. */
    const val APP_STORE_ID = ""

    fun playStoreHttpsUrl(packageName: String = ANDROID_PACKAGE): String =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun appStoreHttpsUrl(appleId: String = APP_STORE_ID): String? {
        val digits = appleId.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return "https://apps.apple.com/app/id$digits"
    }

    fun shareText(
        intro: String,
        androidLabel: String,
        iosLabel: String,
        playUrl: String,
        appStoreUrl: String?,
    ): String = buildString {
        if (intro.isNotBlank()) {
            append(intro.trim())
            append('\n')
        }
        append(androidLabel.trim())
        append(' ')
        append(playUrl)
        if (appStoreUrl != null) {
            append('\n')
            append(iosLabel.trim())
            append(' ')
            append(appStoreUrl)
        }
    }
}
