package com.md7parvez.voiceassistant

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import java.io.File
import java.util.Locale

data class AssistantContext(val sensors: String, val xploreActive: Boolean = false)

interface AssistantEngine {
    fun processUserInput(text: String, context: AssistantContext): String
    fun close() {}
}

class LocalAssistantEngine(private val appContext: Context) : AssistantEngine {
    private val prefs = appContext.getSharedPreferences("offline_memory", Context.MODE_PRIVATE)
    private val memoryKey = "learned_facts"
    private val modelName = "SmolLM2_135M_Instruct.litertlm"

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var initialized = false

    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I didn't hear anything."

        val n = input.lowercase(Locale.getDefault())
        if (n.startsWith("remember that ")) return learn(input.substringAfter("remember that ").trim())
        if (n.startsWith("learn this:")) return learn(input.substringAfter(":").trim())
        if (n.contains("what do you remember") || n.contains("show my memory")) return recall()
        if (n.contains("forget everything") || n.contains("clear memory")) {
            prefs.edit().remove(memoryKey).apply()
            return "Okay. I cleared my offline memory."
        }

        return try {
            ensureModelLoaded()
            val sensorsText = context.sensors.ifBlank { "No sensor data available." }
            val xploreText = if (context.xploreActive)
                "Xplore Mode is active. The camera preview is running, but this text model cannot see the camera."
            else "Xplore Mode is inactive."

            val prompt = """
                You are My Field AI, a small private neural-network assistant running entirely on an Android phone.
                You are offline. Be concise, useful, honest and friendly.
                Never claim to have internet access or to see something you cannot see.
                Use the local memory and device context when useful.

                LOCAL MEMORY:
                ${recallForPrompt()}

                DEVICE CONTEXT:
                Sensors: $sensorsText
                $xploreText

                USER:
                $input
            """.trimIndent()

            conversation!!.sendMessage(prompt, maxOutputToken = 128).toString().trim()
                .ifBlank { "The local neural model returned an empty response." }
        } catch (e: Exception) {
            "Local neural AI error: ${e.message ?: "unknown error"}"
        }
    }

    @Synchronized
    private fun ensureModelLoaded() {
        if (initialized) return

        val modelFile = File(appContext.filesDir, modelName)
        if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
            appContext.assets.open(modelName).use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.CPU(),
            cacheDir = File(appContext.cacheDir, "litertlm").absolutePath
        )

        val newEngine = Engine(config)
        newEngine.initialize()
        val newConversation = newEngine.createConversation()

        engine = newEngine
        conversation = newConversation
        initialized = true
    }

    private fun learn(fact: String): String {
        if (fact.isBlank()) return "Tell me what you want me to remember."
        val facts = readFacts().toMutableList()
        if (!facts.any { it.equals(fact, ignoreCase = true) }) {
            facts.add(fact.take(500))
            while (facts.size > 50) facts.removeAt(0)
            prefs.edit().putStringSet(memoryKey, facts.toSet()).apply()
        }
        return "Got it. I learned that: $fact"
    }

    private fun recall(): String {
        val facts = readFacts()
        if (facts.isEmpty()) return "I don't have any learned memories yet."
        return "I remember: " + facts.joinToString("; ").take(1800)
    }

    private fun recallForPrompt(): String {
        val facts = readFacts()
        return if (facts.isEmpty()) "No saved memories." else facts.joinToString("; ").take(2500)
    }

    private fun readFacts(): List<String> =
        prefs.getStringSet(memoryKey, emptySet())?.toList().orEmpty().sorted()

    override fun close() {
        synchronized(this) {
            try { conversation?.close() } catch (_: Exception) {}
            try { engine?.close() } catch (_: Exception) {}
            conversation = null
            engine = null
            initialized = false
        }
    }
}
