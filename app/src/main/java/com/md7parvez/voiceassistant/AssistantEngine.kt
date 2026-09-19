package com.md7parvez.voiceassistant

import android.content.Context
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

    override fun processUserInput(text: String, context: AssistantContext): String {
        val input = text.trim()
        if (input.isEmpty()) return "I didn't hear anything."

        val n = input.lowercase(Locale.getDefault())

        if (n.startsWith("remember that ")) {
            val fact = input.substringAfter("remember that ", "").trim()
            return learn(fact)
        }
        if (n.startsWith("learn this:")) {
            return learn(input.substringAfter(":", "").trim())
        }
        if (n.contains("what do you remember") || n.contains("show my memory")) {
            return recall()
        }
        if (n.contains("forget everything") || n.contains("clear memory")) {
            prefs.edit().remove(memoryKey).apply()
            return "Okay. I cleared my offline memory."
        }

        return when {
            n == "hi" || n == "hello" || n.startsWith("hello ") ->
                "Hello. My Field AI is ready and working fully offline."
            n.contains("your name") ->
                "I'm My Field AI, your offline assistant."
            n.contains("what time") || n == "time" ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) + "."
            n.contains("what date") || n.contains("today's date") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()) + "."
            n.contains("sensor") ->
                "I can access the sensors reported by Android. " + context.sensors.ifBlank { "No sensors were detected." }
            n.contains("xplore") || n.contains("camera") ->
                if (context.xploreActive) "Xplore Mode is active. The camera preview is running, but offline vision intelligence is not connected yet."
                else "Open Xplore Mode to use the camera."
            n.contains("offline") ->
                "Yes. My current assistant engine works without internet."
            n.contains("who am i") || n.contains("what is my name") ->
                findName()
            n.contains("help") ->
                "Try: remember that I like robotics. Or ask me the time, date, sensors, camera status, or what you remember."
            else ->
                answerFromMemory(input)
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
        return "Got it. I learned that: $fact"
    }

    private fun recall(): String {
        val facts = readFacts()
        if (facts.isEmpty()) return "I don't have any learned memories yet. You can say, 'remember that …'."
        return "I remember: " + facts.joinToString("; ").take(1800)
    }

    private fun findName(): String {
        val fact = readFacts().firstOrNull {
            it.lowercase(Locale.getDefault()).contains("my name is")
        }
        return if (fact != null) fact else "You haven't taught me your name yet."
    }

    private fun answerFromMemory(input: String): String {
        val words = input.lowercase(Locale.getDefault())
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 }
            .toSet()
        val relevant = readFacts().filter { fact ->
            words.any { fact.lowercase(Locale.getDefault()).contains(it) }
        }
        return if (relevant.isNotEmpty()) {
            "From what you taught me: " + relevant.joinToString("; ").take(1200)
        } else {
            "I'm an offline starter AI, so my reasoning is limited right now. Teach me facts with 'remember that …', and I can use them later."
        }
    }

    private fun readFacts(): List<String> =
        prefs.getStringSet(memoryKey, emptySet())?.toList().orEmpty().sorted()

    private fun saveFacts(facts: List<String>) {
        prefs.edit().putStringSet(memoryKey, facts.toSet()).apply()
    }
}
