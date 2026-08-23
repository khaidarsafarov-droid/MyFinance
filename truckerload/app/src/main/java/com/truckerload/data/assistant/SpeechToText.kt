package com.truckerload.data.assistant

enum class SpeechToTextError {
    UNAVAILABLE,
    PERMISSION,
    EMPTY,
    FAILED,
}

interface SpeechToTextListener {
    fun onListening()
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onError(error: SpeechToTextError)
}

/**
 * On-device speech recognition. Implementations must not persist or log transcripts.
 */
interface SpeechToText {
    fun start(languageTag: String, listener: SpeechToTextListener)
    fun stop()
    fun destroy()
}
