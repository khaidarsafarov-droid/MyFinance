package com.truckerload.utils

import android.content.Context

/** Haptic and audio feedback for load/goal events. */
object FeedbackManager {

  @Volatile
  private var appContext: Context? = null

  fun init(context: Context) {
    appContext = context.applicationContext
  }

  fun onLoadAdded() {
    val ctx = appContext ?: return
    SoundManager.playLoadAdded(ctx)
    VibrationManager.vibrateLight(ctx)
  }

  fun onGoalReached() {
    val ctx = appContext ?: return
    SoundManager.playGoalReached(ctx)
    VibrationManager.vibrateGoalReached(ctx)
  }
}
