package com.md7parvez.voiceassistant

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class LocalWhisperInputManager {
    private val sampleRate = 16000
    @Volatile private var recording = false
    @Volatile private var recorder: AudioRecord? = null
    private val lock = Any()
    private val recorded = ArrayList<Short>(sampleRate * 12)
    private var captureThread: Thread? = null

    fun start(onError: (String) -> Unit): Boolean {
        if (recording) return false
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            onError("This phone cannot open a 16 kHz microphone stream.")
            return false
        }
        return try {
            val audio = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuffer * 2, 8192)
            )
            if (audio.state != AudioRecord.STATE_INITIALIZED) {
                audio.release()
                onError("Microphone could not be initialized.")
                return false
            }
            synchronized(lock) { recorded.clear() }
            recorder = audio
            recording = true
            audio.startRecording()
            captureThread = Thread {
                val buffer = ShortArray(2048)
                while (recording) {
                    val n = try {
                        audio.read(buffer, 0, buffer.size)
                    } catch (_: Exception) { -1 }
                    if (n > 0) synchronized(lock) {
                        for (i in 0 until n) recorded.add(buffer[i])
                        // Avoid an accidental very long recording on low-memory devices.
                        if (recorded.size > sampleRate * 25) recording = false
                    }
                }
            }.apply { name = "FieldAI-WhisperMic"; start() }
            true
        } catch (e: Exception) {
            recording = false
            recorder?.release()
            recorder = null
            onError("Microphone error: ${e.message ?: "unknown"}")
            false
        }
    }

    fun isRecording(): Boolean = recording

    fun stop(onSamples: (FloatArray) -> Unit, onError: (String) -> Unit) {
        if (!recording && recorder == null) {
            onError("Recording is not active.")
            return
        }
        recording = false
        Thread {
            try {
                captureThread?.join(1500)
                val audio = recorder
                recorder = null
                try { audio?.stop() } catch (_: Exception) {}
                audio?.release()
                val copy = synchronized(lock) { recorded.toShortArray() }
                if (copy.size < sampleRate / 2) {
                    onError("Recording too short. Hold SPEAK and talk for at least one second.")
                    return@Thread
                }
                val floats = FloatArray(copy.size) { copy[it] / 32768.0f }
                onSamples(floats)
            } catch (e: Exception) {
                onError("Could not finish recording: ${e.message ?: "unknown"}")
            }
        }.start()
    }

    fun release() {
        recording = false
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        captureThread = null
    }
}
