package com.truckerload.voice

import android.util.Log
import com.truckerload.utils.CrashReporting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VoiceCommandBus {
    private val _pending = MutableStateFlow<AppVoiceAction?>(null)
    val pending: StateFlow<AppVoiceAction?> = _pending.asStateFlow()

    fun offer(command: AppVoiceAction) {
        VoiceAssistantLogger.log(command, "received")
        _pending.value = command
    }

    fun consume(): AppVoiceAction? {
        val current = _pending.value
        _pending.value = null
        return current
    }
}

object VoicePendingDraft {
    @Volatile
    var chatText: String? = null

    fun consumeChatText(): String? {
        val text = chatText
        chatText = null
        return text
    }
}

object VoiceAssistantLogger {
    private const val TAG = "VoiceAssistant"

    fun log(command: AppVoiceAction, outcome: String, detail: String? = null) {
        val action = when (command) {
            is AppVoiceAction.OpenScreen -> "open/${command.route}"
            is AppVoiceAction.ChatWithFriend -> "chat/${command.peerQuery}"
            is AppVoiceAction.MessageFriend -> "message/${command.peerQuery}"
            is AppVoiceAction.CallFriend -> "call/${command.peerQuery}"
        }
        Log.i(TAG, "action=$action outcome=$outcome ${detail.orEmpty()}")
        CrashReporting.setCustomKey("voice_last_action", action.take(80))
        CrashReporting.setCustomKey("voice_last_outcome", outcome.take(40))
    }

    fun logOutcome(action: String, outcome: String) {
        Log.i(TAG, "action=$action outcome=$outcome")
        CrashReporting.setCustomKey("voice_last_action", action.take(80))
        CrashReporting.setCustomKey("voice_last_outcome", outcome.take(40))
    }
}
