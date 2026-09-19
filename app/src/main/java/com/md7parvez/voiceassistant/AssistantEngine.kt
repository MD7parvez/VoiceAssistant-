package com.md7parvez.voiceassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class AssistantContext(val sensors: String, val xploreActive: Boolean = false)
interface AssistantEngine { fun processUserInput(text: String, context: AssistantContext): String }

class LocalAssistantEngine : AssistantEngine {
    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I didn't hear anything."
        val n = input.lowercase(Locale.getDefault())
        return when {
            n.contains("hello") || n == "hi" -> "Hello. My Field AI is ready."
            n.contains("your name") -> "I'm My Field AI, your assistant."
            n.contains("what time") -> "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
            n.contains("sensor") -> "Device sensors: " + context.sensors.ifBlank { "none detected" } + "."
            n.contains("xplore") || n.contains("camera") ->
                if (context.xploreActive) "Xplore Mode is active. AI vision will be connected in a later update." else "Open Xplore Mode to use the camera."
            n.contains("offline") -> "Offline fallback mode is active."
            else -> "I heard: " + input.take(500) + ". Gemini AI is not configured yet."
        }
    }
}

class GeminiAssistantEngine(
    private val context: Context,
    private val apiKeyProvider: () -> String
) : AssistantEngine {

    private val maxHistory = 12
    private val history = mutableListOf<Pair<String, String>>()

    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I didn't hear anything."

        val key = apiKeyProvider().trim()
        if (key.isEmpty()) return "Gemini is not configured. Add your Gemini API key in AI Settings."

        return try {
            val result = requestGemini(input, context, key)
            history += "user" to input
            history += "model" to result
            while (history.size > maxHistory) history.removeAt(0)
            result
        } catch (e: Exception) {
            "AI connection failed: " + friendlyError(e)
        }
    }

    private fun requestGemini(input: String, assistantContext: AssistantContext, key: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(30).toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", key)
        }

        try {
            val contents = JSONArray()
            history.forEach { (role, text) ->
                contents.put(JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", text))))
            }

            val contextNote = "Device sensor summary: " +
                assistantContext.sensors.ifBlank { "none available" } +
                ". Xplore active: " + assistantContext.xploreActive + "."
            contents.put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", contextNote + "\n\n" + input))))

            val body = JSONObject()
                .put("system_instruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put(
                        "text",
                        "You are My Field AI, a concise Android voice assistant. " +
                            "Answer naturally and accurately. Keep normal answers reasonably short because they may be spoken aloud. " +
                            "Do not claim to see the camera or use sensors beyond the context supplied by the app. " +
                            "If a capability is unavailable, say so clearly."
                    ))
                )
                .put("contents", contents)
                .toString()

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

            if (code !in 200..299) throw IllegalStateException("Gemini HTTP " + code)

            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
                ?: throw IllegalStateException("No AI candidate returned")
            if (candidates.length() == 0) throw IllegalStateException("Empty AI response")

            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?: throw IllegalStateException("AI response had no text")

            val answer = buildString {
                for (i in 0 until parts.length()) {
                    append(parts.optJSONObject(i)?.optString("text").orEmpty())
                }
            }.trim()

            if (answer.isBlank()) throw IllegalStateException("AI returned empty text")
            answer.take(6000)
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyError(error: Exception): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("401") || message.contains("403") -> "check your Gemini API key and permissions."
            message.contains("429") -> "the AI service rate limit was reached; try again shortly."
            message.contains("500") || message.contains("502") || message.contains("503") -> "the AI service is temporarily unavailable."
            message.contains("timeout", true) -> "the request timed out; check your connection."
            else -> "check your internet connection and AI settings."
        }
    }
}
