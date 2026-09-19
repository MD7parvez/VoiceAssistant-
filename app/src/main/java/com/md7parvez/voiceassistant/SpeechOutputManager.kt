package com.md7parvez.voiceassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechOutputManager(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    var ready = false
        private set
    private var released = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        if (released) return

        if (status != TextToSpeech.SUCCESS) {
            ready = false
            return
        }

        // The phone's default locale can be a language for which the installed
        // TTS engine has no voice. Prefer the device locale, then fall back to
        // English so the assistant can still speak on low-end devices.
        val deviceLocale = Locale.getDefault()
        val deviceSupport = tts.isLanguageAvailable(deviceLocale)

        val selectedLocale = if (deviceSupport >= TextToSpeech.LANG_AVAILABLE) {
            deviceLocale
        } else {
            Locale.US
        }

        val selectedSupport = tts.isLanguageAvailable(selectedLocale)
        if (selectedSupport >= TextToSpeech.LANG_AVAILABLE) {
            tts.language = selectedLocale
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.0f)
            ready = true

            pendingText?.let {
                pendingText = null
                speak(it)
            }
        } else {
            ready = false
        }
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}): Boolean {
        val cleanText = text.trim()
        if (released || cleanText.isBlank()) return false

        // TTS initialization is asynchronous. Keep the latest response so it
        // is spoken as soon as the engine becomes ready.
        if (!ready) {
            pendingText = cleanText
            return false
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStart()
            override fun onDone(utteranceId: String?) = onDone()
            override fun onError(utteranceId: String?) = onDone()
        })

        val result = tts.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "assistant-${System.nanoTime()}"
        )

        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        if (!released) {
            pendingText = null
            tts.stop()
        }
    }

    fun release() {
        if (!released) {
            released = true
            ready = false
            pendingText = null
            tts.stop()
            tts.shutdown()
        }
    }
}
