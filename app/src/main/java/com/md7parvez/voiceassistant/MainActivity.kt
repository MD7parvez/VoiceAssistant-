package com.md7parvez.voiceassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.*
import android.hardware.camera2.*

class MainActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var indicator: TextView
    private lateinit var input: EditText
    private lateinit var speech: SpeechInputManager
    private lateinit var output: SpeechOutputManager
    private lateinit var sensors: SensorManagerService
    private lateinit var engine: AssistantEngine
    private lateinit var modelStatus: TextView
    private lateinit var speakButton: Button
    private val whisperInput = LocalWhisperInputManager()
    private var whisperRecording = false
    private val modelCode = 12
    private var state = AssistantState.IDLE
    private var xplore = false
    private var surface: SurfaceView? = null
    private var camera: CameraDevice? = null
    private val micCode = 10
    private val cameraCode = 11

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speech = SpeechInputManager(this)
        output = SpeechOutputManager(this)
        sensors = SensorManagerService(this)
        engine = LocalAssistantEngine(applicationContext)
        buildMain()
    }

    private fun tv(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        setPadding(4, 8, 4, 8)
    }

    private fun buildMain() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.rgb(10, 15, 18))
        }
        root.addView(tv("MY FIELD AI", 28f))
        root.addView(tv("Offline neural voice assistant • v0.4", 13f))

        indicator = tv("●  IDLE", 26f).apply {
            gravity = 1
            setTextColor(Color.rgb(128, 203, 196))
        }
        root.addView(indicator, LinearLayout.LayoutParams(-1, 0, 0.6f))

        status = tv("Local-first AI • no cloud speech recognition", 14f)
        root.addView(status)

        modelStatus = tv(modelStatusText(), 13f)
        root.addView(modelStatus)
        root.addView(Button(this).apply {
            text = "LOAD WHISPER MODEL (.BIN)"
            setOnClickListener { openModelPicker() }
        })

        transcript = tv(
            "Speak or type a message.\n\n" +
                "Teach me with: “remember that I like robotics.”\n" +
                "Ask: “what do you remember?”",
            16f
        )
        root.addView(transcript, LinearLayout.LayoutParams(-1, 0, 1.5f))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        input = EditText(this).apply {
            hint = "Type a message…"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(false)
        }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "SEND"
            setOnClickListener { sendTypedMessage() }
        })
        root.addView(row)

        speakButton = Button(this).apply {
            text = if (whisperRecording) "STOP & TRANSCRIBE" else "SPEAK • LOCAL WHISPER"
            setOnClickListener { startVoice() }
        }
        root.addView(speakButton)
        root.addView(Button(this).apply {
            text = "CLEAR MEMORY"
            setOnClickListener {
                engine.processUserInput("clear memory", AssistantContext(sensors.summary(), xplore))
                transcript.append("\n\nSystem: Offline memory cleared.")
            }
        })
        root.addView(Button(this).apply {
            text = "XPLORE • CAMERA"
            setOnClickListener { openXplore() }
        })
        root.addView(tv(
            "The neural model runs locally on the phone. Voice output uses the phone's installed TTS engine.",
            12f
        ))
        setContentView(root)
    }

    private fun sendTypedMessage() {
        val text = input.text.toString().trim()
        if (text.isBlank()) return
        input.text.clear()
        transcript.text = "You: $text"
        processWithAi(text)
    }

    private fun processWithAi(text: String) {
        state = AssistantState.PROCESSING
        render()
        Thread {
            val response = engine.processUserInput(
                text,
                AssistantContext(sensors.summary(), xplore)
            )
            runOnUiThread {
                transcript.append("\n\nMy Field AI: $response")
                if (output.ready) {
                    state = AssistantState.SPEAKING
                    render()
                    val started = output.speak(response, {}, {
                        runOnUiThread {
                            state = if (xplore) AssistantState.XPLORE else AssistantState.IDLE
                            render()
                        }
                    })
                    if (!started) {
                        state = if (xplore) AssistantState.XPLORE else AssistantState.IDLE
                        status.text = "Neural text response ready — voice output unavailable"
                        render()
                    }
                } else {
                    state = if (xplore) AssistantState.XPLORE else AssistantState.IDLE
                    status.text = "Neural text response ready — TTS unavailable"
                    render()
                }
            }
        }.start()
    }

    private fun startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), micCode)
            return
        }
        val modelPath = getSharedPreferences("whisper_model", MODE_PRIVATE).getString("path", null)
        if (modelPath.isNullOrBlank() || !File(modelPath).exists()) {
            openModelPicker()
            status.text = "Choose a whisper.cpp-compatible model file first."
            return
        }

        if (whisperRecording) {
            whisperRecording = false
            speakButton.text = "PROCESSING WHISPER…"
            speakButton.isEnabled = false
            status.text = "Transcribing locally. This may take a while on ARM32."
            whisperInput.stop({ samples ->
                Thread {
                    val result = try {
                        WhisperNative.transcribe(modelPath, samples, "auto")
                    } catch (e: UnsatisfiedLinkError) {
                        "ERROR: Local Whisper native engine failed to load: " + (e.message ?: "unknown")
                    } catch (e: Exception) {
                        "ERROR: Whisper failed: " + (e.message ?: "unknown")
                    }
                    runOnUiThread {
                        speakButton.isEnabled = true
                        speakButton.text = "SPEAK • LOCAL WHISPER"
                        if (result.startsWith("ERROR:")) {
                            status.text = result.removePrefix("ERROR:").trim()
                            state = AssistantState.ERROR
                            render()
                        } else {
                            transcript.text = "You: $result"
                            processWithAi(result)
                        }
                    }
                }.start()
            }, { message ->
                runOnUiThread {
                    speakButton.isEnabled = true
                    speakButton.text = "SPEAK • LOCAL WHISPER"
                    status.text = message
                    state = AssistantState.ERROR
                    render()
                }
            })
            render()
            return
        }

        val started = whisperInput.start { message ->
            runOnUiThread {
                status.text = message
                state = AssistantState.ERROR
                render()
            }
        }
        if (started) {
            whisperRecording = true
            speakButton.text = "STOP & TRANSCRIBE"
            status.text = "Recording on-device… tap STOP & TRANSCRIBE when finished."
            state = AssistantState.LISTENING
            render()
        }
    }

    private fun modelStatusText(): String {
        val path = getSharedPreferences("whisper_model", MODE_PRIVATE).getString("path", null)
        val file = path?.let { File(it) }
        return if (file != null && file.exists()) {
            "Whisper model loaded: ${file.name} (${file.length() / (1024 * 1024)} MB)"
        } else {
            "No Whisper model selected. Import a whisper.cpp ggml .bin model."
        }
    }

    private fun openModelPicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            startActivityForResult(intent, modelCode)
        } catch (e: Exception) {
            status.text = "File picker unavailable: " + (e.message ?: "unknown error")
        }
    }

    @Deprecated("Uses the Android document picker for compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != modelCode || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        Thread {
            try {
                var displayName = "whisper-model.bin"
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) displayName = cursor.getString(index) ?: displayName
                    }
                }
                if (!displayName.lowercase().endsWith(".bin")) {
                    runOnUiThread { status.text = "Unsupported file. Choose a whisper.cpp ggml .bin model, not a PyTorch .bin file." }
                    return@Thread
                }
                val target = File(filesDir, "whisper-model.bin")
                contentResolver.openInputStream(uri).use { input ->
                    if (input == null) throw IllegalStateException("Could not open selected file")
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                if (target.length() < 10L * 1024L * 1024L) {
                    target.delete()
                    runOnUiThread { status.text = "That file is too small to be a Whisper model. Choose a converted ggml model." }
                    return@Thread
                }
                getSharedPreferences("whisper_model", MODE_PRIVATE).edit().putString("path", target.absolutePath).apply()
                runOnUiThread {
                    modelStatus.text = modelStatusText()
                    status.text = "Whisper model imported. It will run on this phone without internet."
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Model import failed: " + (e.message ?: "unknown error") }
            }
        }.start()
    }

    private fun render() {
        indicator.text = "●  " + state.name
        if (state != AssistantState.ERROR) {
            status.text = when (state) {
                AssistantState.LISTENING -> "Listening…"
                AssistantState.PROCESSING -> "Thinking offline…"
                AssistantState.SPEAKING -> "Speaking…"
                AssistantState.XPLORE -> "Xplore Mode active"
                AssistantState.IDLE -> "100% offline mode — no API key required"
                else -> status.text
            }
        }
    }

    private fun openXplore() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraCode)
            return
        }
        xplore = true
        state = AssistantState.XPLORE
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        surface = SurfaceView(this)
        surface!!.holder.addCallback(this)
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }
        panel.addView(TextView(this).apply {
            text = "MY FIELD AI • XPLORE"
            textSize = 22f
            setTextColor(Color.WHITE)
        })
        panel.addView(Button(this).apply {
            text = "EXIT XPLORE"
            setOnClickListener { closeXplore() }
        })
        root.addView(panel)
        setContentView(root)
    }

    private fun closeXplore() {
        camera?.close()
        camera = null
        xplore = false
        state = AssistantState.IDLE
        buildMain()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull() ?: throw Exception("Camera unavailable")
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) {
                    camera = c
                    c.createCaptureSession(listOf(holder.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            val request = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(holder.surface)
                            }.build()
                            s.setRepeatingRequest(request, null, null)
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) {
                            runOnUiThread { if (::status.isInitialized) status.text = "Camera preview configuration failed" }
                        }
                    }, null)
                }
                override fun onDisconnected(c: CameraDevice) { c.close() }
                override fun onError(c: CameraDevice, error: Int) {
                    c.close()
                    runOnUiThread { if (::status.isInitialized) status.text = "Camera unavailable" }
                }
            }, null)
        } catch (_: Exception) {
            if (::status.isInitialized) status.text = "Camera unavailable"
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera?.close()
        camera = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grants)
        if (requestCode == micCode && grants.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoice()
        else if (requestCode == cameraCode && grants.firstOrNull() == PackageManager.PERMISSION_GRANTED) openXplore()
        else if (::status.isInitialized) status.text = "Permission denied; feature unavailable"
    }

    override fun onDestroy() {
        speech.stop()
        whisperInput.release()
        output.release()
        sensors.stop()
        camera?.close()
        super.onDestroy()
    }
}
