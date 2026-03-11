package com.lhzkml.codestudio.ui.model

import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.viewmodel.SettingsUiState

internal data class SettingsUiModel(
    val providerName: String,
    val providerDisplayName: String,
    val apiKey: String,
    val modelId: String,
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
    return SettingsUiModel(
        providerName = provider.name,
        providerDisplayName = provider.displayName,
        apiKey = apiKey,
        modelId = modelId,
        baseUrl = baseUrl,
        baseUrlLabel = provider.baseUrlLabel,
        extraConfig = extraConfig,
        extraFieldLabel = provider.extraFieldLabel,
        extraFieldDefault = provider.extraFieldDefault,
        runtimePresetId = runtimePreset.id,
        runtimePresetTitle = runtimePreset.title,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxIterations = maxIterations,
        requiresApiKey = provider.requiresApiKey,
        requiresBaseUrl = provider == Provider.AZURE_OPENAI || provider == Provider.OLLAMA,
        requiresExtraConfig = provider.extraFieldLabel != null
    )
}
