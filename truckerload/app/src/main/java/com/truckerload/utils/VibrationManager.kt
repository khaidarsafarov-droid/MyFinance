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

  /** Light tap for tabs and a saved load. Frequent, so it stays soft. */
  fun vibrateTick(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, oneShotMs = HapticPulse.TICK_MS, amplitude = HapticPulse.TICK_AMPLITUDE)
  }

  /** Heavier pulse for delete. Not used while typing a rate. */
  fun vibrateConfirm(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(
      context,
      oneShotMs = HapticPulse.CONFIRM_MS,
      amplitude = HapticPulse.CONFIRM_AMPLITUDE,
    )
  }

  /** Short tap — used for settings preview only. */
  fun preview(context: Context) {
    if (!isEnabled(context)) return
    vibrateOnMain(context, oneShotMs = 60)
  }

  private fun vibrateOnMain(
    context: Context,
    pattern: LongArray? = null,
    oneShotMs: Long? = null,
    amplitude: Int = HapticPulse.DEFAULT_AMPLITUDE,
  ) {
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
                VibrationEffect.createOneShot(oneShotMs, amplitude),
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

/** Tick stays soft. Delete and the goal pulse stay stronger. */
internal object HapticPulse {
  const val DEFAULT_AMPLITUDE = -1
  const val TICK_MS = 18L
  const val TICK_AMPLITUDE = 40
  const val CONFIRM_MS = 42L
  const val CONFIRM_AMPLITUDE = 210
}
