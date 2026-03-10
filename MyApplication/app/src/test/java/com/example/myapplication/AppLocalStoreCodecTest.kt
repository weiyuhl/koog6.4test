package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocalStoreCodecTest {
    @Test
    fun settings_round_trip_preserves_all_fields() {
        val settings = StoredSettings(
            providerName = "OPENROUTER",
            apiKey = "key-123",
            modelId = "openai/gpt-4o-mini",
            baseUrl = "https://openrouter.ai/api/v1",
            extraConfig = "2024-10-21",
            promptDraft = "你好\n请继续",
            localWriterEnabled = true,
            debuggerEnabled = true,
            debuggerPort = "50901",
            debuggerWaitMs = "750",
            remoteClientEnabled = true,
            remoteHost = "10.0.2.2",
            remotePort = "50901",
            reflectBridgeEnabled = true,
            reflectBridgeBaseUrl = "http://10.0.2.2:8095",
            systemPrompt = "Always cite the provider.",
            temperature = "0.7",
            maxIterations = "25",
        )

        val decoded = AppLocalStoreCodec.decodeSettings(AppLocalStoreCodec.encodeSettings(settings))

        assertEquals(settings, decoded)
    }

    @Test
    fun message_round_trip_preserves_special_characters() {
        val messages = listOf(
            StoredChatMessage(1L, "User", "hello? a=b&c", null),
            StoredChatMessage(2L, "Assistant", "line1\nline2", "执行日志"),
        )

        val decoded = AppLocalStoreCodec.decodeMessages(AppLocalStoreCodec.encodeMessages(messages))

        assertEquals(messages, decoded)
    }

    @Test
    fun invalid_settings_payload_returns_null() {
        assertNull(AppLocalStoreCodec.decodeSettings("broken-payload"))
    }

    @Test
    fun legacy_settings_payload_still_decodes_with_feature_defaults() {
        val legacy = AppLocalStoreCodec.encodeSettings(
            StoredSettings(
                providerName = "OPENAI",
                apiKey = "key",
                modelId = "gpt-4o-mini",
                baseUrl = "https://api.openai.com",
                extraConfig = "",
                promptDraft = "hello",
            )
        ).split("\t").take(6).joinToString("\t")

        val decoded = AppLocalStoreCodec.decodeSettings(legacy)

        assertEquals(true, decoded?.localWriterEnabled)
        assertEquals(false, decoded?.debuggerEnabled)
        assertEquals("50881", decoded?.debuggerPort)
        assertEquals("127.0.0.1", decoded?.remoteHost)
        assertEquals(false, decoded?.reflectBridgeEnabled)
        assertEquals("http://10.0.2.2:8095", decoded?.reflectBridgeBaseUrl)
        assertEquals("", decoded?.systemPrompt)
        assertEquals("0.2", decoded?.temperature)
        assertEquals("50", decoded?.maxIterations)
    }
}