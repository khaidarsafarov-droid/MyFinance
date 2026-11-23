package com.truckerload.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Opens the system battery-optimization list. The user finds TruckoRig and chooses
 * "Don't optimize" or "Unrestricted". This does not use
 * [Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS], which Play treats as a
 * sensitive permission.
 */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Opens the system list when battery optimization is still on.
     * Falls back to this app's details screen, then to general Settings, when
     * nothing resolves the previous screen.
     */
    fun requestIgnoreBatteryOptimizations(context: Context): Boolean {
        if (isIgnoringBatteryOptimizations(context)) return false
        for (intent in batteryScreens(context)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (start(context, intent)) return true
        }
        return false
    }

    private fun batteryScreens(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
        },
        Intent(Settings.ACTION_SETTINGS),
    )

    /**
     * [Intent.resolveActivity] is not used. On API 30+ it hides Settings unless
     * the manifest queries that package, even when [Context.startActivity] works.
     * [ActivityNotFoundException] is the signal that nothing resolved the intent.
     */
    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
