package com.lhzkml.codestudio

internal enum class Route(val value: String) {
    Chat("chat"),
    Home("settings/home"),
    Model("settings/model"),
    Runtime("settings/runtime"),
    OssLicensesList("oss_licenses_list"),
    OssLicensesDetail("oss_licenses_detail/{name}"),
}

internal object RouteHelper {
    fun ossLicensesDetail(name: String) = "oss_licenses_detail/$name"
}

enum class MessageRole {
    User,
    Assistant,
    System,
}

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String,
    val label: String? = null,
    val isStreaming: Boolean = false,
)

internal data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
)

// 数据存储模型
data class StoredSettings(
    val providerName: String,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
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



