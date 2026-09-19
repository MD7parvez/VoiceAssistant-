package com.md7parvez.voiceassistant

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAssistantEngineTest {
    private val engine = LocalAssistantEngine(); private val context = AssistantContext("accelerometer")
    @Test fun greetingIsDeterministic() = assertEquals("Hello. I'm ready.", engine.processUserInput("hello", context))
    @Test fun emptyInputIsHandled() = assertEquals("I didn't hear anything.", engine.processUserInput(" ", context))
    @Test fun sensorsAreReported() = assertEquals("Available sensors: accelerometer.", engine.processUserInput("what sensors are available", context))
    @Test fun unknownInputDoesNotCrash() = assertEquals("I heard you say: xyz. More AI features are not enabled yet.", engine.processUserInput("xyz", context))
}
