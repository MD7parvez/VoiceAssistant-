package com.md7parvez.voiceassistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechInputManager(private val context: Context) {
    interface Listener { fun onListening(); fun onResult(text: String); fun onError(message: String) }
    private var recognizer: SpeechRecognizer? = null
    fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)
    fun start(listener: Listener) {
        if (!isAvailable()) { listener.onError("Speech recognition is unavailable"); return }
        stop(); recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { listener.onListening() }; override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(v: Float) = Unit; override fun onBufferReceived(b: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit; override fun onPartialResults(b: Bundle?) = Unit
                override fun onEvent(t: Int, b: Bundle?) = Unit
                override fun onResults(b: Bundle?) { val text = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull(); if (text.isNullOrBlank()) listener.onError("No speech detected") else listener.onResult(text) }
                override fun onError(e: Int) { listener.onError(when (e) { 6 -> "Please try again"; 7 -> "No speech detected"; 8 -> "Speech service busy"; 9 -> "Microphone permission is required"; else -> "Speech recognition failed" }) }
            })
            r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) })
        }
    }
    fun stop() { recognizer?.destroy(); recognizer = null }
}
