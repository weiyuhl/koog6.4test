package com.example.myapplication

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSettingsLogicTest {
    @Test
    fun validate_native_settings_reports_required_fields() {
        val errors = validateNativeSettings(
            NativeSettingsState(
                provider = KoogProvider.OPENAI,
                apiKey = "",
                modelId = "",
                baseUrl = "https://api.openai.com/v1",
                extraConfig = "",
                promptDraft = "",
                runtimePreset = AgentRuntimePreset.StreamingWithTools,
                systemPrompt = "",
                temperature = "0.2",
                maxIterations = "50",
                codeToolsEnabled = true,
                codeToolsWorkspaceRoot = "",
                codeToolsAllowedPathPrefixes = "",
            )
        )

        assertTrue(errors.hasAny())
        assertTrue(nativeSettingsSummary(errors).contains("请输入模型 ID"))
        assertTrue(nativeSettingsSummary(errors).contains("请先输入 API Key"))
        assertTrue(nativeSettingsSummary(errors).contains("Workspace root"))
    }

    @Test
    fun validate_native_settings_accepts_complete_config() {
        val errors = validateNativeSettings(
            NativeSettingsState(
                provider = KoogProvider.OPENAI,
                apiKey = "key",
                modelId = "gpt-4.1-mini",
                baseUrl = "https://api.openai.com/v1",
                extraConfig = "",
                promptDraft = "hello",
                runtimePreset = AgentRuntimePreset.StreamingWithTools,
                systemPrompt = "system",
                temperature = "0.4",
                maxIterations = "20",
                codeToolsEnabled = true,
                codeToolsWorkspaceRoot = "d:/koog",
                codeToolsAllowedPathPrefixes = "d:/koog",
            )
        )

        assertFalse(errors.hasAny())
    }
}