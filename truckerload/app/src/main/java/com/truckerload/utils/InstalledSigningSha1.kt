package com.truckerload.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** SHA-1 of the installed APK signing cert — the value Google Cloud must list. */
object InstalledSigningSha1 {

    fun fingerprint(context: Context): String? = runCatching {
        val pm = context.packageManager
        val pkg = context.packageName
        val signature = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
            info.signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
        } ?: return@runCatching null
        format(MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()))
    }.getOrNull()

    internal fun format(digest: ByteArray): String =
        digest.joinToString(":") { b -> "%02X".format(b) }
}
