package com.example.myapplication

import android.content.Context

class AppLocalStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): StoredAppState {
        val settings = AppLocalStoreCodec.decodeSettings(prefs.getString(KEY_SETTINGS, null))
            ?: defaultSettings()
        val messages = AppLocalStoreCodec.decodeMessages(prefs.getString(KEY_MESSAGES, null))
            .ifEmpty { defaultMessages() }
        return StoredAppState(settings = settings, messages = messages)
    }

    fun saveSettings(settings: StoredSettings) {
        prefs.edit().putString(KEY_SETTINGS, AppLocalStoreCodec.encodeSettings(settings)).apply()
    }

    fun saveMessages(messages: List<StoredChatMessage>) {
        prefs.edit().putString(KEY_MESSAGES, AppLocalStoreCodec.encodeMessages(messages)).apply()
    }

    fun loadLastWorkspaceRoute(): String? = prefs.getString(KEY_WORKSPACE_ROUTE, null)

    fun saveLastWorkspaceRoute(route: String) {
        prefs.edit().putString(KEY_WORKSPACE_ROUTE, route).apply()
    }

    fun loadRuntimePresetId(): String? = prefs.getString(KEY_RUNTIME_PRESET, null)

    fun saveRuntimePresetId(presetId: String) {
        prefs.edit().putString(KEY_RUNTIME_PRESET, presetId).apply()
    }

    private fun defaultSettings(): StoredSettings = StoredSettings(
        providerName = KoogProvider.OPENAI.name,
        apiKey = "",
        modelId = KoogProvider.OPENAI.defaultModelId,
        baseUrl = KoogProvider.OPENAI.defaultBaseUrl,
        extraConfig = KoogProvider.OPENAI.extraFieldDefault,
        promptDraft = "",
        localWriterEnabled = true,
        debuggerEnabled = false,
        debuggerPort = "50881",
        debuggerWaitMs = "250",
        remoteClientEnabled = false,
        remoteHost = "127.0.0.1",
        remotePort = "50881",
        reflectBridgeEnabled = false,
        reflectBridgeBaseUrl = "http://10.0.2.2:8095",
        systemPrompt = "",
        temperature = "0.2",
        maxIterations = "50",
    )

    private fun defaultMessages(): List<StoredChatMessage> = listOf(
        StoredChatMessage(
            id = 0L,
            role = "System",
            label = "欢迎",
            text = "这里是聊天首页。点左上角按钮打开侧边栏，再从侧边栏底部进入设置页配置供应商。",
        )
    )

    private companion object {
        const val PREFS_NAME = "koog_chat_local_store"
        const val KEY_SETTINGS = "settings"
        const val KEY_MESSAGES = "messages"
        const val KEY_WORKSPACE_ROUTE = "workspace_route"
        const val KEY_RUNTIME_PRESET = "runtime_preset"
    }
}