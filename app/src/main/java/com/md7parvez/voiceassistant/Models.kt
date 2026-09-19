package com.md7parvez.voiceassistant

interface VisionProcessor { fun describe(): String }
class NullVisionProcessor : VisionProcessor { override fun describe() = "Vision processing is not enabled in Version 0.1." }
enum class AssistantState { IDLE, LISTENING, PROCESSING, SPEAKING, XPLORE, ERROR }
