package com.lhzkml.codestudio.ui.model

import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.viewmodel.SettingsUiState

internal data class SettingsUiModel(
    val providerName: String,
    val providerDisplayName: String,
    val apiKey: String,
    val modelId: String,
    val defaultModelId: String,
    val baseUrl: String,
    val baseUrlLabel: String,
    val extraConfig: String,
    val extraFieldLabel: String?,
    val extraFieldDefault: String,
    val runtimePresetId: String,
    val runtimePresetTitle: String,
    val systemPrompt: String,
    val temperature: String,
    val maxIterations: String,
    val requiresApiKey: Boolean,
    val requiresBaseUrl: Boolean,
    val requiresExtraConfig: Boolean
)

internal fun SettingsUiState.toUiModel(): SettingsUiModel {
    val safeProvider = provider ?: Provider.OPENAI
    return SettingsUiModel(
        providerName = safeProvider.name,
        providerDisplayName = safeProvider.displayName,
        apiKey = apiKey,
        modelId = modelId,
        defaultModelId = safeProvider.defaultModelId,
        baseUrl = baseUrl,
        baseUrlLabel = safeProvider.baseUrlLabel,
        extraConfig = extraConfig,
        extraFieldLabel = safeProvider.extraFieldLabel,
        extraFieldDefault = safeProvider.extraFieldDefault,
        runtimePresetId = runtimePreset.id,
        runtimePresetTitle = runtimePreset.title,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxIterations = maxIterations,
        requiresApiKey = safeProvider.requiresApiKey,
        requiresBaseUrl = false, // 所有供应商都使用可选的 Base URL
        requiresExtraConfig = safeProvider.extraFieldLabel != null
    )
}
