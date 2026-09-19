package com.md7parvez.voiceassistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechInputManager(context: Context) {
    interface Listener { fun onListening(); fun onResult(text: String); fun onError(message: String) }
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(callback: Listener) {
        listener = callback
        if (!isAvailable()) { callback.onError("Speech recognition is unavailable"); return }
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { speechRecognizer ->
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listener?.onListening() }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(results: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (text.isNullOrBlank()) listener?.onError("No speech detected") else listener?.onResult(text)
                    clearSession()
                }
                override fun onError(error: Int) {
                    listener?.onError(errorMessage(error))
                    clearSession()
                }
            })
            speechRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        }
    }

    fun stop() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        listener = null
    }

    private fun clearSession() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was cancelled"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service network error"
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
        SpeechRecognizer.ERROR_SERVER -> "Speech service error"
        else -> "Speech recognition failed"
    }
}
