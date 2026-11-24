package com.truckerload.presentation.startup

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.truckerload.data.local.DatabasePassphraseStore
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs cold-start work that the first screen does not need after that screen's first frame.
 *
 * When an activity resumes, the block waits for the next frame and then one more main-loop
 * turn. A start with no activity (a receiver or a worker) runs it after [FALLBACK_DELAY_MS].
 * The block runs once, on the main thread. Robolectric runs it at once so tests see it.
 */
object AfterFirstFrame {

    const val FALLBACK_DELAY_MS = 5_000L

    fun run(app: Application, block: () -> Unit) {
        if (DatabasePassphraseStore.isRobolectric()) {
            block()
            return
        }
        val done = AtomicBoolean(false)
        val main = Handler(Looper.getMainLooper())
        val once = Runnable { if (done.compareAndSet(false, true)) block() }
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                app.unregisterActivityLifecycleCallbacks(this)
                Choreographer.getInstance().postFrameCallback { main.post(once) }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        app.registerActivityLifecycleCallbacks(callbacks)
        main.postDelayed({
            app.unregisterActivityLifecycleCallbacks(callbacks)
            once.run()
        }, FALLBACK_DELAY_MS)
    }
}
