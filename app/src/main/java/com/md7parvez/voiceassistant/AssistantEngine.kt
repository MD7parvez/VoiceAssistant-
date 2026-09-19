package com.md7parvez.voiceassistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantContext(val sensors: String, val xploreActive: Boolean = false)
interface AssistantEngine { fun processUserInput(text: String, context: AssistantContext): String }

class LocalAssistantEngine : AssistantEngine {
    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I didn't hear anything."
        val normalized = input.lowercase(Locale.getDefault())
        return when {
            normalized.contains("hello") || normalized == "hi" -> "Hello. I'm ready."
            normalized.contains("your name") -> "I'm VoiceAssistant."
            normalized.contains("what time") -> "The time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
            normalized.contains("sensor") -> "Available sensors: ${context.sensors.ifBlank { "none detected" }}."
            else -> "I heard you say: ${input.take(500)}. More AI features are not enabled yet."
        }
    }
}
