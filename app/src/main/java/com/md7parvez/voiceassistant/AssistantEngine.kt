package com.md7parvez.voiceassistant

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantContext(val sensors: String, val xploreActive: Boolean = false)

interface AssistantEngine {
    fun processUserInput(text: String, context: AssistantContext): String
}

class LocalAssistantEngine(private val appContext: Context) : AssistantEngine {
    private val prefs = appContext.getSharedPreferences("offline_memory", Context.MODE_PRIVATE)
    private val memoryKey = "learned_facts"
    @Volatile private var llm: LlmInference? = null
    @Volatile private var modelError: String? = null

    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I did not hear anything."
        val n = input.lowercase(Locale.getDefault())

        if (n.startsWith("remember that ")) return learn(input.substringAfter("remember that ").trim())
        if (n.startsWith("learn this:")) return learn(input.substringAfter(":").trim())
        if (n.contains("what do you remember") || n.contains("show my memory")) return recall()
        if (n.contains("forget everything") || n.contains("clear memory")) {
            prefs.edit().remove(memoryKey).apply()
            return "Okay. I cleared my offline memory."
        }

        return when {
            n == "hi" || n == "hello" || n.startsWith("hello ") ->
                "Hello. My Field AI is ready. My neural language model runs locally on this phone."
            n.contains("your name") ->
                "I am My Field AI, your offline neural assistant."
            n.contains("what time") || n == "time" ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
            n.contains("what date") || n.contains("todays date") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."
            n.contains("sensor") ->
                "I can access the sensors reported by Android. " + context.sensors.ifBlank { "No sensors were detected." }
            n.contains("xplore") || n.contains("camera") ->
                if (context.xploreActive) "Xplore Mode is active. The camera preview is running. Neural vision is not enabled yet."
                else "Open Xplore Mode to use the camera."
            n.contains("offline") ->
                "Yes. The neural language model is packaged with this build and runs on the phone without an internet connection."
            n.contains("who am i") || n.contains("what is my name") -> findName()
            n.contains("help") ->
                "You can talk naturally to me, or teach me facts with remember that. You can also ask about time, date, sensors, memory, and Xplore Mode."
            else -> generateNeuralResponse(input, context)
        }
    }

    private fun generateNeuralResponse(input: String, context: AssistantContext): String {
        val engine = getLlm() ?: return modelError ?: "The local neural model could not be initialized."
        val memory = readFacts()
        val memoryText = if (memory.isEmpty()) "No stored memories." else memory.joinToString("; ").take(1200)
        val prompt = buildString {
            append("You are My Field AI, a small offline voice assistant running entirely on an Android phone. ")
            append("Be helpful, concise, and honest. Do not claim to see or hear anything unless the app provides it. ")
            append("Android sensor summary: ")
            append(context.sensors.ifBlank { "No sensor data available." })
            append(". Xplore camera mode is ")
            append(if (context.xploreActive) "active" else "inactive")
            append(". Stored user memories: ")
            append(memoryText)
            append(". Answer the user directly. User: ")
            append(input)
        }

        return try {
            val result = engine.generateResponse(prompt).trim()
            if (result.isBlank()) "I generated an empty response. Please try again." else result
        } catch (e: Exception) {
            modelError = "The neural model could not generate a response: " + (e.message ?: "unknown error")
            modelError!!
        }
    }

    @Synchronized
    private fun getLlm(): LlmInference? {
        llm?.let { return it }
        modelError?.let { return null }

        return try {
            val modelFile = File(appContext.filesDir, "SmolLM-135M-Instruct.task")
            if (!modelFile.exists()) {
                appContext.assets.open("SmolLM-135M-Instruct.task").use { input ->
                    modelFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(128)
                .setTemperature(0.7f)
                .setRandomSeed(42)
                .build()

            LlmInference.createFromOptions(appContext, options).also { llm = it }
        } catch (e: Exception) {
            modelError = "Local neural AI failed to initialize: " + (e.message ?: "unknown error")
            null
        }
    }

    private fun learn(fact: String): String {
        if (fact.isBlank()) return "Tell me what you want me to remember."
        val facts = readFacts().toMutableList()
        if (!facts.any { it.equals(fact, ignoreCase = true) }) {
            facts.add(fact.take(500))
            while (facts.size > 50) facts.removeAt(0)
            saveFacts(facts)
        }
        return "Got it. I learned that: " + fact
    }

    private fun recall(): String {
        val facts = readFacts()
        if (facts.isEmpty()) return "I do not have any learned memories yet."
        return "I remember: " + facts.joinToString("; ").take(1800)
    }

    private fun findName(): String {
        val fact = readFacts().firstOrNull { it.lowercase(Locale.getDefault()).contains("my name is") }
        return if (fact != null) fact else "You have not taught me your name yet."
    }

    private fun readFacts(): List<String> =
        prefs.getStringSet(memoryKey, emptySet())?.toList().orEmpty().sorted()

    private fun saveFacts(facts: List<String>) {
        prefs.edit().putStringSet(memoryKey, facts.toSet()).apply()
    }
}
