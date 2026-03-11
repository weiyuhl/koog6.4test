package com.lhzkml.codestudio

internal enum class Route(val value: String) {
    Chat("chat"),
    Home("settings/home"),
    Model("settings/model"),
    Runtime("settings/runtime"),
}

internal enum class MessageRole {
    User,
    Assistant,
    System,
}

internal data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String,
    val label: String? = null,
)

internal data class FormErrors(
    val provider: String? = null,
    val modelId: String? = null,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val extraConfig: String? = null,
    val temperature: String? = null,
    val maxIterations: String? = null,
) {
    fun hasAny(): Boolean = listOf(
        provider,
        modelId,
        apiKey,
        baseUrl,
        extraConfig,
        temperature,
        maxIterations,
    ).any { it != null }
}

internal const val STREAMING_PLACEHOLDER = "正在思考..."

internal fun StoredChatMessage.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.System),
    text = text,
    label = label,
)

internal fun ChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    role = role.name,
    text = text,
    label = label,
)


