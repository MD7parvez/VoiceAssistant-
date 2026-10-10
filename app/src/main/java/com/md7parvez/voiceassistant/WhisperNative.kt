package com.md7parvez.voiceassistant

object WhisperNative {
    init {
        System.loadLibrary("fieldai_whisper")
    }

    @JvmStatic
    external fun transcribe(modelPath: String, audioSamples: FloatArray, language: String): String
}
