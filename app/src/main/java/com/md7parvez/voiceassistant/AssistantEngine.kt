package com.md7parvez.voiceassistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantContext(val sensors: String, val xploreActive: Boolean = false)
interface AssistantEngine { fun processUserInput(text: String, context: AssistantContext): String }
class LocalAssistantEngine : AssistantEngine {
    override fun processUserInput(text: String, context: AssistantContext): String = when {
        text.trim().isEmpty() -> "I didn't hear anything."
        text.trim().lowercase().contains("hello") || text.trim().lowercase().contains("hi") -> "Hello. I'm ready."
        text.lowercase().contains("your name") -> "I'm VoiceAssistant."
        text.lowercase().contains("what time") -> "The time is ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
        text.lowercase().contains("sensor") -> "Available sensors: ${context.sensors}."
        else -> "I heard you say: $text. More AI features are not enabled yet."
    }
}
