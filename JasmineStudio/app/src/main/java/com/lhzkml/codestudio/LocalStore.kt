package com.lhzkml.codestudio

import android.content.Context

class LocalStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): StoredState {
        val settings = StoreCodec.decodeSettings(prefs.getString(KEY_SETTINGS, null))
            ?: defaultSettings()
        val messages = StoreCodec.decodeMessages(prefs.getString(KEY_MESSAGES, null))
            .ifEmpty { defaultMessages() }
        return StoredState(settings = settings, messages = messages)
    }

    fun saveSettings(settings: StoredSettings) {
        prefs.edit().putString(KEY_SETTINGS, StoreCodec.encodeSettings(settings)).apply()
    }

    fun saveMessages(messages: List<StoredChatMessage>) {
        prefs.edit().putString(KEY_MESSAGES, StoreCodec.encodeMessages(messages)).apply()
    }

    fun loadRuntimePresetId(): String? = prefs.getString(KEY_RUNTIME_PRESET, null)

    fun saveRuntimePresetId(presetId: String) {
        prefs.edit().putString(KEY_RUNTIME_PRESET, presetId).apply()
    }

    private fun defaultSettings(): StoredSettings = StoredSettings(
        providerName = Provider.OPENAI.name,
        apiKey = "",
        modelId = Provider.OPENAI.defaultModelId,
        baseUrl = Provider.OPENAI.defaultBaseUrl,
        extraConfig = Provider.OPENAI.extraFieldDefault,
        promptDraft = "",
        systemPrompt = "",
        temperature = "0.2",
        maxIterations = "50",
    )

    private fun defaultMessages(): List<StoredChatMessage> = listOf(
        StoredChatMessage(
            id = 0L,
            role = "System",
            label = "欢迎",
            text = "欢迎使用。请先点左上角菜单，在抽屉底部进入设置完成模型配置，然后开始聊天",
        )
    )

    private companion object {
        const val PREFS_NAME = "koog_chat_local_store"
        const val KEY_SETTINGS = "settings"
        const val KEY_MESSAGES = "messages"
        const val KEY_RUNTIME_PRESET = "runtime_preset"
    }
}
