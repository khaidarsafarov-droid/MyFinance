package com.truckerload.utils

import androidx.core.content.edit
import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object SoundManager {

  private const val PREFS = "truckerload_settings"
  private const val KEY_SOUND_ENABLED = "feedback_sound_enabled"

  private val mainHandler = Handler(Looper.getMainLooper())

  fun isEnabled(context: Context): Boolean =
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .getBoolean(KEY_SOUND_ENABLED, true)

  fun setEnabled(context: Context, enabled: Boolean) {
    context.applicationContext
      .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
      .edit {
          putBoolean(KEY_SOUND_ENABLED, enabled)
      }
  }

  fun playLoadAdded(context: Context) {
    if (!isEnabled(context)) return
    playOnMain(context) { playNotificationTone(it, durationMs = 180) }
  }

  fun playGoalReached(context: Context) {
    if (!isEnabled(context)) return
    playOnMain(context) { playNotificationTone(it, durationMs = 450, goal = true) }
  }

  /** Preview from settings when the user turns sound on. */
  fun preview(context: Context) {
    if (!isEnabled(context)) return
    playLoadAdded(context)
  }

  private fun playOnMain(context: Context, block: (Context) -> Unit) {
    val app = context.applicationContext
    if (Looper.myLooper() == Looper.getMainLooper()) {
      block(app)
    } else {
      mainHandler.post { block(app) }
    }
  }

  private fun playNotificationTone(context: Context, durationMs: Int, goal: Boolean = false) {
    if (playRingtone(context, goal)) return
    runCatching { playToneGenerator(durationMs, goal) }
  }

  private fun playRingtone(context: Context, goal: Boolean): Boolean {
    val type = if (goal) {
      RingtoneManager.TYPE_ALARM
    } else {
      RingtoneManager.TYPE_NOTIFICATION
    }
    val uri = RingtoneManager.getDefaultUri(type)
      ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      ?: return false
    val ringtone: Ringtone = RingtoneManager.getRingtone(context, uri) ?: return false
    ringtone.audioAttributes = android.media.AudioAttributes.Builder()
      .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
      .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    ringtone.play()
    return true
  }

  private fun playToneGenerator(durationMs: Int, goal: Boolean) {
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    val toneType = if (goal) {
      ToneGenerator.TONE_CDMA_CONFIRM
    } else {
      ToneGenerator.TONE_PROP_ACK
    }
    tone.startTone(toneType, durationMs)
    mainHandler.postDelayed({ tone.release() }, (durationMs + 80).toLong())
  }
}
