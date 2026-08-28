package com.truckerload.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Current runtime permission flags shown in Settings.
 * Re-read after [androidx.lifecycle.Lifecycle.Event.ON_RESUME] so system Settings
 * grants appear without restarting the app.
 */
data class RuntimePermissionSnapshot(
    val cameraGranted: Boolean,
    val locationGranted: Boolean,
    val notificationsGranted: Boolean,
) {
    companion object {
        fun fromFlags(
            camera: Boolean,
            fineLocation: Boolean,
            coarseLocation: Boolean,
            sdkInt: Int,
            postNotifications: Boolean,
        ): RuntimePermissionSnapshot = RuntimePermissionSnapshot(
            cameraGranted = camera,
            locationGranted = fineLocation || coarseLocation,
            notificationsGranted = if (sdkInt >= 33) postNotifications else true,
        )

        fun from(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): RuntimePermissionSnapshot =
            fromFlags(
                camera = granted(context, Manifest.permission.CAMERA),
                fineLocation = granted(context, Manifest.permission.ACCESS_FINE_LOCATION),
                coarseLocation = granted(context, Manifest.permission.ACCESS_COARSE_LOCATION),
                sdkInt = sdkInt,
                postNotifications = granted(context, Manifest.permission.POST_NOTIFICATIONS),
            )

        private fun granted(context: Context, permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}
