package com.truckerload.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {

  private const val PREFS = "truckerload_settings"
  private const val KEY_SOUND_ENABLED = "feedback_sound_enabled"

  fun isEnabled(context: Context): Boolean =
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .getBoolean(KEY_SOUND_ENABLED, true)

  fun setEnabled(context: Context, enabled: Boolean) {
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_SOUND_ENABLED, enabled)
      .apply()
  }

  fun playLoadAdded(context: Context) {
    if (!isEnabled(context)) return
    runCatching {
      val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
      try {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
      } finally {
        tone.release()
      }
    }
  }

  fun playGoalReached(context: Context) {
    if (!isEnabled(context)) return
    runCatching {
      val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
      try {
        tone.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 450)
      } finally {
        tone.release()
      }
    }
  }
}
