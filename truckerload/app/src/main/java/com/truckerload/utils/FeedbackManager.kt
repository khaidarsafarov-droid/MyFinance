package com.truckerload.utils

import android.content.Context
import android.os.Handler
import android.os.Looper

/** Haptic and audio feedback for load/goal events. */
object FeedbackManager {

  @Volatile
  private var appContext: Context? = null

  private val mainHandler = Handler(Looper.getMainLooper())

  fun init(context: Context) {
    appContext = context.applicationContext
  }

  fun onLoadAdded() {
    val ctx = appContext ?: return
    mainHandler.post {
      SoundManager.playLoadAdded(ctx)
      VibrationManager.vibrateTick(ctx)
    }
  }

  fun onGoalReached() {
    val ctx = appContext ?: return
    mainHandler.post {
      SoundManager.playGoalReached(ctx)
      VibrationManager.vibrateGoalReached(ctx)
    }
  }

  fun onDeleteGesture() {
    val ctx = appContext ?: return
    mainHandler.post {
      VibrationManager.vibrateConfirm(ctx)
    }
  }

  fun onNavSelect() {
    val ctx = appContext ?: return
    mainHandler.post {
      VibrationManager.vibrateTick(ctx)
    }
  }
}
