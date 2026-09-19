package com.md7parvez.voiceassistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.camera2.*
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.*

class MainActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var indicator: TextView
    private lateinit var speech: SpeechInputManager
    private lateinit var output: SpeechOutputManager
    private lateinit var sensors: SensorManagerService
    private val engine = LocalAssistantEngine()
    private var state = AssistantState.IDLE
    private var xplore = false
    private var surface: SurfaceView? = null
    private var camera: CameraDevice? = null
    private val micCode = 10
    private val cameraCode = 11

    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        speech = SpeechInputManager(this)
        output = SpeechOutputManager(this)
        sensors = SensorManagerService(this)
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
        root.addView(tv("Offline field intelligence • v0.2", 13f))

        indicator = tv("●  IDLE", 26f).apply {
            gravity = 1
            setTextColor(Color.rgb(128, 203, 196))
        }
        root.addView(indicator, LinearLayout.LayoutParams(-1, 0, 0.8f))

        status = tv("Ready — voice assistant online", 14f)
        root.addView(status)

        transcript = tv("Ask about first aid, field skills, engineering, sensors, or simulation planning.", 16f)
        root.addView(transcript, LinearLayout.LayoutParams(-1, 0, 1.4f))

        root.addView(Button(this).apply {
            text = "SPEAK"
            setOnClickListener { startVoice() }
        })
        root.addView(Button(this).apply {
            text = "XPLORE • CAMERA"
            setOnClickListener { openXplore() }
        })
        root.addView(tv("OFFLINE MODE: core assistant and device tools work without internet. Advanced AI models can be added later.", 12f))
        setContentView(root)
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
                state = AssistantState.PROCESSING
                render()
                val response = engine.processUserInput(text, AssistantContext(sensors.summary(), xplore))
                transcript.append("\n\nMy Field AI: $response")
                output.speak(response, {
                    state = AssistantState.SPEAKING
                    render()
                }, {
                    state = if (xplore) AssistantState.XPLORE else AssistantState.IDLE
                    render()
                })
            }
            override fun onError(message: String) {
                status.text = message
                state = AssistantState.ERROR
                render()
            }
        })
    }

    private fun render() {
        indicator.text = "●  " + state.name
        status.text = when (state) {
            AssistantState.LISTENING -> "Listening…"
            AssistantState.PROCESSING -> "Processing…"
            AssistantState.SPEAKING -> "Speaking…"
            AssistantState.XPLORE -> "Xplore Mode active"
            AssistantState.ERROR -> "An error occurred"
            else -> "Ready — offline"
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
                        override fun onConfigureFailed(s: CameraCaptureSession) {}
                    }, null)
                }
                override fun onDisconnected(c: CameraDevice) { c.close() }
                override fun onError(c: CameraDevice, error: Int) { c.close() }
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