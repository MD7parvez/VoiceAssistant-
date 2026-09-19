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

    override fun onInit(status: Int) {
        if (released || status != TextToSpeech.SUCCESS) return
        ready = tts.isLanguageAvailable(Locale.getDefault()) >= TextToSpeech.LANG_AVAILABLE
        if (ready) tts.language = Locale.getDefault()
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}): Boolean {
        if (!ready || released || text.isBlank()) return false
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStart()
            override fun onDone(utteranceId: String?) = onDone()
            override fun onError(utteranceId: String?) = onDone()
        })
        return tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "assistant-${System.nanoTime()}") == TextToSpeech.SUCCESS
    }

    fun stop() { if (!released) tts.stop() }

    fun release() {
        if (!released) {
            released = true
            ready = false
            tts.stop()
            tts.shutdown()
        }
    }
}
