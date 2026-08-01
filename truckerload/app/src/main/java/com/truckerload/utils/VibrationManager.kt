package com.truckerload.utils

import androidx.core.content.edit
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationManager {

  private const val PREFS = "truckerload_settings"
  private const val KEY_VIBRATION_ENABLED = "feedback_vibration_enabled"

  private val mainHandler = Handler(Looper.getMainLooper())

  fun isEnabled(context: Context): Boolean =
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .getBoolean(KEY_VIBRATION_ENABLED, true)

  fun setEnabled(context: Context, enabled: Boolean) {
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit {
          putBoolean(KEY_VIBRATION_ENABLED, enabled)
      }
  }

  fun vibrateGoalReached(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, pattern = longArrayOf(0, 120, 80, 160))
  }

  /** Light confirmation tap (save, successful action). */
  fun vibrateConfirm(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, oneShotMs = 40)
  }

  /** Distinct tick for destructive / swipe gestures. */
  fun vibrateWarning(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, pattern = longArrayOf(0, 35, 40, 35))
  }

  /** Short tap — used for settings preview only. */
  fun preview(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, oneShotMs = 60)
  }

  private fun vibrateOnMain(context: Context, pattern: LongArray? = null, oneShotMs: Long? = null) {
    val app = context.applicationContext
    val action: () -> Unit = {
      val vibrator = vibrator(app)
      if (vibrator != null && vibrator.hasVibrator()) {
        runCatching {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when {
              pattern != null -> vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1),
              )
              oneShotMs != null -> vibrator.vibrate(
                VibrationEffect.createOneShot(oneShotMs, VibrationEffect.DEFAULT_AMPLITUDE),
              )
            }
          } else {
            @Suppress("DEPRECATION")
            when {
              pattern != null -> vibrator.vibrate(pattern, -1)
              oneShotMs != null -> vibrator.vibrate(oneShotMs)
            }
          }
        }
      }
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action()
    } else {
      mainHandler.post(action)
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
