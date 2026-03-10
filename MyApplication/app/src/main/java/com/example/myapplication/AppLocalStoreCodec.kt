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
    val localWriterEnabled: Boolean = true,
    val debuggerEnabled: Boolean = false,
    val debuggerPort: String = "50881",
    val debuggerWaitMs: String = "250",
    val remoteClientEnabled: Boolean = false,
    val remoteHost: String = "127.0.0.1",
    val remotePort: String = "50881",
    val reflectBridgeEnabled: Boolean = false,
    val reflectBridgeBaseUrl: String = "http://10.0.2.2:8095",
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
        settings.localWriterEnabled.toString(),
        settings.debuggerEnabled.toString(),
        settings.debuggerPort,
        settings.debuggerWaitMs,
        settings.remoteClientEnabled.toString(),
        settings.remoteHost,
        settings.remotePort,
        settings.reflectBridgeEnabled.toString(),
        settings.reflectBridgeBaseUrl,
        settings.systemPrompt,
        settings.temperature,
        settings.maxIterations,
    ).joinToString(separator = "\t", transform = ::escape)

    fun decodeSettings(raw: String?): StoredSettings? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split("\t")
        if (parts.size != 6 && parts.size != 13 && parts.size != 15 && parts.size != 18) return null

        return StoredSettings(
            providerName = unescape(parts[0]),
            apiKey = unescape(parts[1]),
            modelId = unescape(parts[2]),
            baseUrl = unescape(parts[3]),
            extraConfig = unescape(parts[4]),
            promptDraft = unescape(parts[5]),
            localWriterEnabled = parts.getOrNull(6)?.let(::unescape)?.toBooleanStrictOrNull() ?: true,
            debuggerEnabled = parts.getOrNull(7)?.let(::unescape)?.toBooleanStrictOrNull() ?: false,
            debuggerPort = parts.getOrNull(8)?.let(::unescape) ?: "50881",
            debuggerWaitMs = parts.getOrNull(9)?.let(::unescape) ?: "250",
            remoteClientEnabled = parts.getOrNull(10)?.let(::unescape)?.toBooleanStrictOrNull() ?: false,
            remoteHost = parts.getOrNull(11)?.let(::unescape) ?: "127.0.0.1",
            remotePort = parts.getOrNull(12)?.let(::unescape) ?: "50881",
            reflectBridgeEnabled = parts.getOrNull(13)?.let(::unescape)?.toBooleanStrictOrNull() ?: false,
            reflectBridgeBaseUrl = parts.getOrNull(14)?.let(::unescape) ?: "http://10.0.2.2:8095",
            systemPrompt = parts.getOrNull(15)?.let(::unescape).orEmpty(),
            temperature = parts.getOrNull(16)?.let(::unescape) ?: "0.2",
            maxIterations = parts.getOrNull(17)?.let(::unescape) ?: "50",
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