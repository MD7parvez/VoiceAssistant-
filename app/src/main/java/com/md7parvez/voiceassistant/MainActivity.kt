package com.md7parvez.voiceassistant

import android.Manifest
import android.app.Activity
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
    private var state = AssistantState.IDLE
    private var xplore = false
    private var surface: SurfaceView? = null
    private var camera: CameraDevice? = null
    private val micCode = 10
    private val cameraCode = 11
    private val prefs by lazy { getSharedPreferences("ai_settings", MODE_PRIVATE) }

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        speech = SpeechInputManager(this)
        output = SpeechOutputManager(this)
        sensors = SensorManagerService(this)
        engine = GeminiAssistantEngine(this) { prefs.getString("gemini_api_key", "").orEmpty() }
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
        root.addView(tv("Voice-first AI assistant • v0.2", 13f))

        indicator = tv("●  IDLE", 26f).apply {
            gravity = 1
            setTextColor(Color.rgb(128, 203, 196))
        }
        root.addView(indicator, LinearLayout.LayoutParams(-1, 0, 0.6f))

        status = tv(if (hasApiKey()) "AI ready — text and voice available" else "AI key needed — tap AI SETTINGS", 14f)
        root.addView(status)

        transcript = tv("Type a message or press SPEAK. AI responses appear here even if voice output is unavailable.", 16f)
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

        root.addView(Button(this).apply {
            text = "SPEAK"
            setOnClickListener { startVoice() }
        })
        root.addView(Button(this).apply {
            text = "AI SETTINGS"
            setOnClickListener { showAiSettings() }
        })
        root.addView(Button(this).apply {
            text = "XPLORE • CAMERA"
            setOnClickListener { openXplore() }
        })
        root.addView(tv("Text reply is always shown. Voice reply is optional and depends on the phone's TTS service.", 12f))
        setContentView(root)
    }

    private fun hasApiKey() = prefs.getString("gemini_api_key", "").orEmpty().isNotBlank()

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
            val response = engine.processUserInput(text, AssistantContext(sensors.summary(), xplore))
            runOnUiThread {
                transcript.append("\n\nMy Field AI: $response")
                // Text is committed to the UI before TTS is attempted.
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
                        status.text = "Text response ready — voice output unavailable"
                        render()
                    }
                } else {
                    state = if (xplore) AssistantState.XPLORE else AssistantState.IDLE
                    status.text = "Text response ready — TTS unavailable"
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
        state = AssistantState.LISTENING
        render()
        speech.start(object : SpeechInputManager.Listener {
            override fun onListening() { status.text = "Listening…" }
            override fun onResult(text: String) {
                transcript.text = "You: $text"
                processWithAi(text)
            }
            override fun onError(message: String) {
                status.text = message
                state = AssistantState.ERROR
                render()
            }
        })
    }

    private fun showAiSettings() {
        val field = EditText(this).apply {
            hint = "Paste Gemini API key"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini AI settings")
            .setMessage("For this prototype, the key is stored only in this app's private preferences. Do not paste a key into GitHub or source code.")
            .setView(field)
            .setPositiveButton("SAVE") { _, _ ->
                val key = field.text.toString().trim()
                prefs.edit().putString("gemini_api_key", key).apply()
                status.text = if (key.isBlank()) "Gemini key cleared" else "Gemini key saved on this device"
            }
            .setNegativeButton("CANCEL", null)
            .setNeutralButton("CLEAR") { _, _ ->
                prefs.edit().remove("gemini_api_key").apply()
                status.text = "Gemini key cleared"
            }
            .show()
    }

    private fun render() {
        indicator.text = "●  " + state.name
        if (state != AssistantState.ERROR) {
            status.text = when (state) {
                AssistantState.LISTENING -> "Listening…"
                AssistantState.PROCESSING -> "Thinking with Gemini…"
                AssistantState.SPEAKING -> "Speaking…"
                AssistantState.XPLORE -> "Xplore Mode active"
                AssistantState.IDLE -> if (hasApiKey()) "AI ready" else "AI key needed — tap AI SETTINGS"
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
                            runOnUiThread { status.text = "Camera preview configuration failed" }
                        }
                    }, null)
                }
                override fun onDisconnected(c: CameraDevice) { c.close() }
                override fun onError(c: CameraDevice, error: Int) {
                    c.close()
                    runOnUiThread { status.text = "Camera unavailable" }
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
        output.release()
        sensors.stop()
        camera?.close()
        super.onDestroy()
    }
}
