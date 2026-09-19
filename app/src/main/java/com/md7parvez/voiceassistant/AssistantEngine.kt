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
        val n = input.lowercase(Locale.getDefault())
        return when {
            n.contains("hello") || n == "hi" -> "Hello. My Field AI is ready."
            n.contains("your name") -> "I'm My Field AI, an offline field assistant."
            n.contains("what time") -> "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
            n.contains("sensor") -> "Device sensors: " + context.sensors.ifBlank { "none detected" } + "."
            n.contains("first aid") || n.contains("medical") || n.contains("paramedic") ->
                "I can provide general first-aid education, but serious emergencies need qualified medical help and local emergency services."
            n.contains("military") || n.contains("mission plan") || n.contains("strategy") ->
                "I can help with historical, fictional, and tabletop strategy simulations. I won't provide instructions for carrying out real-world violence."
            n.contains("gun") || n.contains("firearm") || n.contains("weapon") ->
                "For a real weapon, prioritize safety: keep it pointed in a safe direction, keep your finger off the trigger, and seek qualified professional instruction. I can discuss general safety and history."
            n.contains("xplore") || n.contains("camera") ->
                if (context.xploreActive) "Xplore Mode is active. Camera analysis will be added in a later version." else "Open Xplore Mode to use the camera."
            n.contains("offline") -> "Core My Field AI features are designed to work offline. A larger local model can be integrated later."
            else -> "I heard: " + input.take(500) + ". Core offline features are active; advanced local AI is planned for the next build."
        }
    }
}