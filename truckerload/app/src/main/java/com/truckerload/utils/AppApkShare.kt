package com.truckerload.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.truckerload.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Share this installed APK with a friend (sideload). Data stays on each device.
 */
object AppApkShare {
    private const val TAG = "AppApkShare"
    private const val CACHE_NAME = "TruckoRig-share.apk"

    /**
     * Copies the installed package APK into cache and opens a share sheet.
     * @return false when the source APK cannot be read or shared.
     */
    fun shareInstalledApk(context: Context): Boolean {
        val app = context.applicationContext
        return try {
            val sourcePath = app.applicationInfo.sourceDir ?: return false
            val source = File(sourcePath)
            if (!source.exists() || !source.canRead()) return false
            val dest = File(app.cacheDir, CACHE_NAME)
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            val uri = FileProvider.getUriForFile(
                app,
                "${app.packageName}.fileprovider",
                dest,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(app.contentResolver, "apk", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            app.startActivity(
                Intent.createChooser(
                    intent,
                    app.getString(R.string.settings_share_app_chooser),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "shareInstalledApk failed", e)
            false
        }
    }
}
