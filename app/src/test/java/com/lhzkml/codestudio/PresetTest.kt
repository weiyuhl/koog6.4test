package com.lhzkml.codestudio

import org.junit.Assert.assertEquals
import org.junit.Test

class PresetTest {
    @Test
    fun fromId_returns_streaming_default_for_unknown_value() {
        assertEquals(Preset.GraphToolsSequential, Preset.fromId("missing"))
    }

    @Test
    fun fromId_returns_matching_preset_when_value_is_known() {
        assertEquals(
            Preset.GraphToolsParallel,
            Preset.fromId(Preset.GraphToolsParallel.id),
        )
    }
}
