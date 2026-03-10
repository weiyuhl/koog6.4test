package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentRuntimePresetTest {
    @Test
    fun fromId_returns_streaming_default_for_unknown_value() {
        assertEquals(AgentRuntimePreset.StreamingWithTools, AgentRuntimePreset.fromId("missing"))
    }

    @Test
    fun fromId_returns_matching_preset_when_value_is_known() {
        assertEquals(
            AgentRuntimePreset.GraphConditionalRouting,
            AgentRuntimePreset.fromId(AgentRuntimePreset.GraphConditionalRouting.id),
        )
    }
}