package com.example.myapplication

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class StoredSettings(
    val providerName: String,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val promptDraft: String,
    val systemPrompt: String = "",
    val temperature: String = "0.2",
    val maxIterations: String = "50",
)

data class StoredChatMessage(
    val id: Long,
    val role: String,
    val text: String,
    val label: String?,
)

data class StoredAppState(
    val settings: StoredSettings,
    val messages: List<StoredChatMessage>,
)

object AppLocalStoreCodec {
    fun encodeSettings(settings: StoredSettings): String = listOf(
        settings.providerName,
        settings.apiKey,
        settings.modelId,
        settings.baseUrl,
        settings.extraConfig,
        settings.promptDraft,
        settings.systemPrompt,
        settings.temperature,
        settings.maxIterations,
    ).joinToString(separator = "\t", transform = ::escape)

    fun decodeSettings(raw: String?): StoredSettings? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("\t")
        if (parts.size != 9) return null

        return StoredSettings(
            providerName = unescape(parts[0]),
            apiKey = unescape(parts[1]),
            modelId = unescape(parts[2]),
            baseUrl = unescape(parts[3]),
            extraConfig = unescape(parts[4]),
            promptDraft = unescape(parts[5]),
            systemPrompt = parts.getOrNull(6)?.let(::unescape).orEmpty(),
            temperature = parts.getOrNull(7)?.let(::unescape) ?: "0.2",
            maxIterations = parts.getOrNull(8)?.let(::unescape) ?: "50",
        )
    }

    fun encodeMessages(messages: List<StoredChatMessage>): String = messages.joinToString(separator = "\n") { message ->
        listOf(
            message.id.toString(),
            escape(message.role),
            escape(message.label.orEmpty()),
            escape(message.text),
        ).joinToString(separator = "\t")
    }

    fun decodeMessages(raw: String?): List<StoredChatMessage> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::decodeMessageLine)
            .toList()
    }

    private fun decodeMessageLine(line: String): StoredChatMessage? {
        val parts = line.split("\t")
        if (parts.size != 4) return null

        return StoredChatMessage(
            id = parts[0].toLongOrNull() ?: return null,
            role = unescape(parts[1]),
            label = unescape(parts[2]).ifBlank { null },
            text = unescape(parts[3]),
        )
    }

    private fun escape(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun unescape(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
}