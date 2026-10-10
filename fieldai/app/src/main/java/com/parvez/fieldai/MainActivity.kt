package com.parvez.fieldai

import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var status: TextView
    private lateinit var conversation: TextView
    private lateinit var question: EditText
    private lateinit var camera: PreviewView
    private var modelLoaded = false
    private val worker = Executors.newSingleThreadExecutor()
    private val chooseModel = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importAndLoadModel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        camera = PreviewView(this).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        val root = FrameLayout(this)
        root.addView(camera, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 12)
            setBackgroundColor(0xB9000000.toInt())
        }
        top.addView(TextView(this).apply {
            text = "FIELD AI  /  LOCAL INFERENCE"
            textSize = 20f; setTextColor(0xFFB8FFDA.toInt())
        })
        status = TextView(this).apply {
            text = "AI engine not loaded · Choose a compatible .gguf model"
            textSize = 13f; setTextColor(-1)
        }
        top.addView(status)
        top.addView(Button(this).apply {
            text = "IMPORT LOCAL AI MODEL (.GGUF)"
            setOnClickListener { chooseModel.launch(arrayOf("application/octet-stream", "*/*")) }
        })
        root.addView(top, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xD9000000.toInt())
        }
        bottom.addView(TextView(this).apply {
            text = "AR TEXT CONSOLE  ·  Answers appear over the live camera"
            textSize = 14f; setTextColor(0xFFB8FFDA.toInt())
        })
        val scroll = ScrollView(this)
        conversation = TextView(this).apply {
            text = "Field AI ready. Load a small instruct GGUF model, then type a question.\n\nNote: this build runs local text AI; it does not yet analyse camera pixels."
            textSize = 15f; setTextColor(-1); setPadding(4, 8, 4, 8)
        }
        scroll.addView(conversation)
        bottom.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        question = EditText(this).apply {
            hint = "Ask Field AI…"
            setTextColor(-1); setHintTextColor(0xFFBBBBBB.toInt())
            setSingleLine(false); maxLines = 3
            setBackgroundColor(0x66333333)
        }
        bottom.addView(question)
        bottom.addView(Button(this).apply {
            text = "ASK LOCAL AI"
            setOnClickListener { askLocalAi() }
        })
        root.addView(bottom, FrameLayout.LayoutParams(-1, 390, Gravity.BOTTOM))
        setContentView(root)
        startCamera()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(camera.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                status.text = if (modelLoaded) "Camera ready · local LLM loaded" else "Camera ready · AI model not loaded"
            } catch (e: Exception) { status.text = "Camera error: ${e.message}" }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun importAndLoadModel(uri: Uri) {
        status.text = "Copying model to app storage… large files may take a while"
        conversation.append("\n\nSystem: Importing selected model…")
        worker.execute {
            try {
                val modelFile = File(filesDir, "fieldai-model.gguf")
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Cannot open selected file" }.use { src ->
                        modelFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                }
                runOnUiThread { status.text = "Loading local model… first load can be slow" }
                val loaded = LocalLlm.nativeLoad(modelFile.absolutePath)
                modelLoaded = loaded
                runOnUiThread {
                    status.text = if (loaded) "LOCAL AI LOADED · answers run on this phone" else "Model failed to load. Try a smaller, valid GGUF instruct model."
                    conversation.append(if (loaded) "\nSystem: Local model loaded successfully." else "\nSystem: Model load failed.")
                }
            } catch (e: Throwable) {
                runOnUiThread {
                    status.text = "Import/load error: ${e.message ?: e.javaClass.simpleName}"
                    conversation.append("\nSystem error: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun askLocalAi() {
        val userText = question.text.toString().trim()
        if (userText.isEmpty()) return
        question.setText("")
        conversation.append("\n\nYOU: $userText\n\nFIELD AI: Thinking locally…")
        if (!modelLoaded) {
            conversation.append("\nLoad a compatible GGUF model first.")
            return
        }
        status.text = "Running local inference… keep the app open"
        worker.execute {
            try {
                val prompt = "You are Field AI, a concise offline field assistant. Answer clearly and do not claim to see the camera. If uncertain, say so.\n\nUser: $userText\nAssistant:"
                val answer = LocalLlm.nativeAsk(prompt).trim().ifBlank { "The model returned an empty answer. Try another instruct GGUF model." }
                runOnUiThread {
                    conversation.append("\n$answer")
                    status.text = "Local answer generated · no network request used"
                }
            } catch (e: Throwable) {
                runOnUiThread { status.text = "AI inference error: ${e.message ?: e.javaClass.simpleName}"; conversation.append("\nAI error: ${e.message}") }
            }
        }
    }

    override fun onDestroy() {
        try { if (modelLoaded) LocalLlm.nativeUnload() } catch (_: Throwable) {}
        worker.shutdownNow()
        super.onDestroy()
    }
}
