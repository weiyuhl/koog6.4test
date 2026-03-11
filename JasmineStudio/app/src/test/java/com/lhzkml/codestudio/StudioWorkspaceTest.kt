package com.lhzkml.codestudio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSettingsLogicTest {
    @Test
    fun validate_native_settings_reports_required_fields() {
        val errors = validateSettings(
            State(
                provider = Provider.OPENAI,
                apiKey = "",
                modelId = "",
                baseUrl = "https://api.openai.com/v1",
                extraConfig = "",
                promptDraft = "",
                runtimePreset = Preset.GraphToolsSequential,
                systemPrompt = "",
                temperature = "0.2",
                maxIterations = "50",
            )
        )

        assertTrue(errors.hasAny())
        assertTrue(settingsSummary(errors).contains("请输入模型 ID"))
        assertTrue(settingsSummary(errors).contains("请先输入 API Key"))
    }

    @Test
    fun validate_native_settings_accepts_complete_config() {
        val errors = validateSettings(
            State(
                provider = Provider.OPENAI,
                apiKey = "key",
                modelId = "gpt-4.1-mini",
                baseUrl = "https://api.openai.com/v1",
                extraConfig = "",
                promptDraft = "hello",
                runtimePreset = Preset.GraphToolsSequential,
                systemPrompt = "system",
                temperature = "0.4",
                maxIterations = "20",
            )
        )

        assertFalse(errors.hasAny())
    }
}
