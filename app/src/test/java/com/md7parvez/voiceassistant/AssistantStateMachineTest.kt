package com.md7parvez.voiceassistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantStateMachineTest {
    @Test fun validVoiceFlowReachesSpeaking() {
        val machine = AssistantStateMachine()
        assertTrue(machine.beginListening()); assertTrue(machine.beginProcessing()); assertTrue(machine.beginSpeaking())
        assertTrue(machine.recover()); assertTrue(machine.state == AssistantState.IDLE)
    }
    @Test fun duplicateListeningIsRejected() {
        val machine = AssistantStateMachine()
        assertTrue(machine.beginListening()); assertFalse(machine.beginListening())
    }
    @Test fun errorCanRecover() {
        val machine = AssistantStateMachine(); machine.fail()
        assertTrue(machine.recover()); assertTrue(machine.state == AssistantState.IDLE)
    }
}
