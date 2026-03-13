package com.lhzkml.codestudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreCodecTest {
    @Test
    fun settings_round_trip_preserves_all_fields() {
        val settings = StoredSettings(
            providerName = "OPENROUTER",
            apiKey = "key-123",
            modelId = "openai/gpt-4o-mini",
            baseUrl = "https://openrouter.ai/api/v1",
            extraConfig = "2024-10-21",
            systemPrompt = "Always cite the provider.",
            temperature = "0.7",
            maxIterations = "25",
        )

        val decoded = StoreCodec.decodeSettings(StoreCodec.encodeSettings(settings))

        assertEquals(settings, decoded)
    }

    @Test
    fun message_round_trip_preserves_special_characters() {
        val messages = listOf(
            StoredChatMessage(1L, "User", "hello? a=b&c", null),
            StoredChatMessage(2L, "Assistant", "line1\nline2", "执行日志"),
        )

        val decoded = StoreCodec.decodeMessages(StoreCodec.encodeMessages(messages))

        assertEquals(messages, decoded)
    }

    @Test
    fun invalid_settings_payload_returns_null() {
        assertNull(StoreCodec.decodeSettings("broken-payload"))
    }

    @Test
    fun legacy_settings_payload_with_promptDraft_can_be_decoded() {
        // 旧格式包含 promptDraft（9个字段）
        val legacy = listOf(
            "OPENAI", "key", "gpt-4o-mini", "https://api.openai.com", "", 
            "hello", // promptDraft (会被忽略)
            "system", "0.3", "12"
        ).joinToString("\t")

        val decoded = StoreCodec.decodeSettings(legacy)
        
        assertEquals("OPENAI", decoded?.providerName)
        assertEquals("key", decoded?.apiKey)
        assertEquals("gpt-4o-mini", decoded?.modelId)
        assertEquals("system", decoded?.systemPrompt)
        assertEquals("0.3", decoded?.temperature)
        assertEquals("12", decoded?.maxIterations)
    }

    @Test
    fun legacy_settings_payload_with_fewer_fields_returns_null() {
        // 只有6个字段的旧格式应该返回 null
        val legacy = StoreCodec.encodeSettings(
            StoredSettings(
                providerName = "OPENAI",
                apiKey = "key",
                modelId = "gpt-4o-mini",
                baseUrl = "https://api.openai.com",
                extraConfig = "",
                systemPrompt = "system",
                temperature = "0.3",
                maxIterations = "12"
            )
        ).split("\t").take(6).joinToString("\t")

        assertNull(StoreCodec.decodeSettings(legacy))
    }
}
