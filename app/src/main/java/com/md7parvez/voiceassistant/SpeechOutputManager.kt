package com.md7parvez.voiceassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechOutputManager(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    var ready = false; private set
    override fun onInit(status: Int) { ready = status == TextToSpeech.SUCCESS; if (ready) tts.language = Locale.getDefault() }
    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) { if (!ready) return; tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() { override fun onStart(id: String?) = onStart(); override fun onDone(id: String?) = onDone(); override fun onError(id: String?) = onDone() }); tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "assistant-${System.currentTimeMillis()}") }
    fun stop() { tts.stop() }
    fun release() { tts.shutdown() }
}
