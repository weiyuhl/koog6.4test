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
    fun legacy_settings_payload_returns_null() {
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

        assertNull(AppLocalStoreCodec.decodeSettings(legacy))
    }

    @Test
    fun old_long_payload_returns_null() {
        val legacy = listOf(
            "OPENAI", "key", "gpt-4o-mini", "https://api.openai.com", "", "hello",
            "system", "0.3", "12",
        ).joinToString("\t")

        // 旧格式应该返回null，因为字段数不匹配
        assertNull(AppLocalStoreCodec.decodeSettings(legacy))
    }
}