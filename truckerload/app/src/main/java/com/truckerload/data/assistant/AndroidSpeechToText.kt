package com.truckerload.data.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class AndroidSpeechToText(
    private val appContext: Context,
) : SpeechToText {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listener: SpeechToTextListener? = null
    private val started = AtomicBoolean(false)

    override fun start(languageTag: String, listener: SpeechToTextListener) {
        this.listener = listener
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                listener.onError(SpeechToTextError.UNAVAILABLE)
                return@post
            }
            val engine = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also {
                recognizer = it
            }
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    started.set(true)
                    listener.onListening()
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    started.set(false)
                    listener.onError(mapError(error))
                }

                override fun onResults(results: Bundle?) {
                    started.set(false)
                    val text = firstResult(results)
                    if (text.isNullOrBlank()) {
                        listener.onError(SpeechToTextError.EMPTY)
                    } else {
                        listener.onFinal(text.trim())
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = firstResult(partialResults)?.trim().orEmpty()
                    if (text.isNotEmpty()) listener.onPartial(text)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            started.set(true)
            engine.startListening(buildIntent(languageTag))
            listener.onListening()
        }
    }

    override fun stop() {
        mainHandler.post {
            runCatching { recognizer?.stopListening() }
            started.set(false)
        }
    }

    override fun destroy() {
        listener = null
        started.set(false)
        mainHandler.post {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    private fun buildIntent(languageTag: String): Intent {
        val locale = when (languageTag.lowercase(Locale.US)) {
            "en" -> Locale.US
            else -> Locale("ru", "RU")
        }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun mapError(error: Int): SpeechToTextError = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechToTextError.PERMISSION
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> SpeechToTextError.EMPTY
        SpeechRecognizer.ERROR_CLIENT,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_AUDIO,
        -> SpeechToTextError.FAILED
        else -> SpeechToTextError.FAILED
    }
}
