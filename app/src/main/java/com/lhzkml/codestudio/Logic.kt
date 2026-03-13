package com.lhzkml.codestudio

import com.lhzkml.codestudio.validation.ValidationService

internal data class State(
    val provider: Provider,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val runtimePreset: Preset,
    val systemPrompt: String,
    val temperature: String,
    val maxIterations: String,
)

internal fun validateSettings(state: State): FormErrors = ValidationService.validateSettings(state)

internal fun settingsSummary(errors: FormErrors): String = buildList {
    errors.provider?.let(::add)
    errors.modelId?.let(::add)
    errors.apiKey?.let(::add)
    errors.baseUrl?.let(::add)
    errors.extraConfig?.let(::add)
    errors.temperature?.let(::add)
    errors.maxIterations?.let(::add)
}.joinToString("；")

internal fun State.toStoredSettings(): StoredSettings = StoredSettings(
    providerName = provider.name,
    apiKey = apiKey,
    modelId = modelId,
    baseUrl = baseUrl,
    extraConfig = extraConfig,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxIterations = maxIterations,
)

internal fun State.toAgentRequest(userPrompt: String): Request = Request(
    provider = provider,
    apiKey = apiKey.trim(),
    modelId = modelId.trim(),
    baseUrl = baseUrl.trim(),
    extraConfig = extraConfig.trim(),
    runtimePreset = runtimePreset,
    systemPrompt = systemPrompt.trim(),
    temperature = temperature.trim().toDoubleOrNull(),
    maxIterations = maxIterations.trim().toIntOrNull(),
    userPrompt = userPrompt.trim(),
)

