package com.truckerload.voice

import android.util.Log
import com.truckerload.utils.CrashReporting
import kotlinx.coroutines.channels.Channel

object VoiceCommandBus {
    private val commands = Channel<AppVoiceAction>(capacity = Channel.UNLIMITED)

    fun offer(command: AppVoiceAction) {
        VoiceAssistantLogger.log(command, "received")
        val result = commands.trySend(command)
        if (result.isFailure) {
            VoiceAssistantLogger.logOutcome(
                "voice",
                "queue_rejected",
                result.exceptionOrNull()?.message,
            )
        }
    }

    suspend fun receive(): AppVoiceAction = commands.receive()

    /** Non-blocking receive for tests and dismiss cleanup. */
    fun tryReceive(): AppVoiceAction? = commands.tryReceive().getOrNull()

    /** Drains any queued commands (test isolation). */
    internal fun drainForTests() {
        while (tryReceive() != null) {
            // discard
        }
    }
}

object VoiceAssistantLogger {
    private const val TAG = "VoiceAssistant"

    fun log(command: AppVoiceAction, outcome: String, detail: String? = null) {
        val action = when (command) {
            is AppVoiceAction.OpenScreen -> "open/${command.route}"
            is AppVoiceAction.AddDiesel -> "journal/add_diesel"
            is AppVoiceAction.AddPaycheck -> "journal/add_paycheck"
            is AppVoiceAction.QueryWeeklyGross -> "journal/weekly_gross"
        }
        logOutcome(action, outcome, detail)
    }

    fun logOutcome(action: String, outcome: String, detail: String? = null) {
        val suffix = detail?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        // android.util.Log is a no-op stub in JVM unit tests.
        runCatching { Log.i(TAG, "action=$action outcome=$outcome$suffix") }
        CrashReporting.setCustomKey("voice_last_action", action.take(80))
        CrashReporting.setCustomKey("voice_last_outcome", outcome.take(40))
    }
}
