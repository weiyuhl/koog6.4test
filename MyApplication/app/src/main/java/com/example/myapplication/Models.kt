package com.example.myapplication

internal enum class NativeRoute(val value: String) {
    Chat("chat"),
    SettingsHome("settings/home"),
    SettingsModel("settings/model"),
    SettingsRuntime("settings/runtime"),
}

internal enum class NativeMessageRole {
    User,
    Assistant,
    System,
}

internal data class NativeChatMessage(
    val id: Long,
    val role: NativeMessageRole,
    val text: String,
    val label: String? = null,
)

internal data class NativeFormErrors(
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

internal const val NATIVE_STREAMING_PLACEHOLDER = "正在思考…"

internal fun StoredChatMessage.toNativeMessage(): NativeChatMessage = NativeChatMessage(
    id = id,
    role = runCatching { NativeMessageRole.valueOf(role) }.getOrDefault(NativeMessageRole.System),
    text = text,
    label = label,
)

internal fun NativeChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    role = role.name,
    text = text,
    label = label,
)

