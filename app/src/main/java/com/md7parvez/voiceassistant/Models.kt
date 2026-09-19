package com.md7parvez.voiceassistant

enum class AssistantState { IDLE, LISTENING, PROCESSING, SPEAKING, XPLORE, ERROR }

class AssistantStateMachine(initial: AssistantState = AssistantState.IDLE) {
    var state: AssistantState = initial
        private set

    fun beginListening(): Boolean = transition(AssistantState.LISTENING, AssistantState.IDLE, AssistantState.XPLORE)
    fun beginProcessing(): Boolean = transition(AssistantState.PROCESSING, AssistantState.LISTENING)
    fun beginSpeaking(): Boolean = transition(AssistantState.SPEAKING, AssistantState.PROCESSING)
    fun enterXplore(): Boolean = transition(AssistantState.XPLORE, AssistantState.IDLE)
    fun recover(): Boolean = transition(AssistantState.IDLE, AssistantState.ERROR, AssistantState.SPEAKING, AssistantState.LISTENING, AssistantState.PROCESSING)
    fun fail(): Boolean { state = AssistantState.ERROR; return true }

    private fun transition(next: AssistantState, vararg allowed: AssistantState): Boolean {
        if (state !in allowed) return false
        state = next
        return true
    }
}
