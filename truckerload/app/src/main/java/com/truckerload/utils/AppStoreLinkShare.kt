package com.truckerload.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.truckerload.R

/** Opens the system share sheet with Play Store (and App Store when listed). */
object AppStoreLinkShare {
    private const val TAG = "AppStoreLinkShare"

    fun share(context: Context): Boolean {
        val app = context.applicationContext
        val text = StoreListings.shareText(
            intro = app.getString(R.string.settings_share_app_message_intro),
            androidLabel = app.getString(R.string.settings_share_app_android_label),
            iosLabel = app.getString(R.string.settings_share_app_ios_label),
            playUrl = StoreListings.playStoreHttpsUrl(app.packageName),
            appStoreUrl = StoreListings.appStoreHttpsUrl(),
        )
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, app.getString(R.string.settings_share_app_chooser))
            }
            app.startActivity(
                Intent.createChooser(
                    intent,
                    app.getString(R.string.settings_share_app_chooser),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "share failed", e)
            false
        }
    }
}
