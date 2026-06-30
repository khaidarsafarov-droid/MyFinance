package com.truckerload.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationManager {

  private const val PREFS = "truckerload_settings"
  private const val KEY_VIBRATION_ENABLED = "feedback_vibration_enabled"

  fun isEnabled(context: Context): Boolean =
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .getBoolean(KEY_VIBRATION_ENABLED, true)

  fun setEnabled(context: Context, enabled: Boolean) {
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_VIBRATION_ENABLED, enabled)
      .apply()
  }

  fun vibrateGoalReached(context: Context) {
    if (!isEnabled(context)) return
    val vibrator = vibrator(context) ?: return
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
          VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1),
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 120, 80, 160), -1)
      }
    }
  }

  fun vibrateLight(context: Context) {
    if (!isEnabled(context)) return
    val vibrator = vibrator(context) ?: return
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(40)
      }
    }
  }

  private fun vibrator(context: Context): Vibrator? {
    val app = context.applicationContext
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val manager = app.getSystemService(VibratorManager::class.java)
      manager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }
}
