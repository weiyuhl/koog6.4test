package com.example.myapplication

internal data class NativeSettingsState(
    val provider: Provider,
    val apiKey: String,
    val modelId: String,
    val baseUrl: String,
    val extraConfig: String,
    val promptDraft: String,
    val runtimePreset: AgentRuntimePreset,
    val systemPrompt: String,
    val temperature: String,
    val maxIterations: String,
)

internal fun validateNativeSettings(state: NativeSettingsState): NativeFormErrors = NativeFormErrors(
    provider = if (!state.provider.isSupportedOnAndroid) "当前 Android 版本暂不支持该供应商，请切换到其他供应商。" else null,
    modelId = if (state.modelId.isBlank()) "请输入模型 ID" else null,
    apiKey = if (state.provider.requiresApiKey && state.apiKey.isBlank()) "请先输入 API Key" else null,
    baseUrl = if ((state.provider == Provider.AZURE_OPENAI || state.provider == Provider.OLLAMA) && state.baseUrl.isBlank()) "该供应商需要 Base URL" else null,
    extraConfig = if (state.provider.extraFieldLabel != null && state.extraConfig.isBlank()) "请填写该供应商所需的额外配置" else null,
    temperature = when {
        state.temperature.isBlank() -> "请输入 temperature"
        state.temperature.toDoubleOrNull() == null -> "temperature 必须是数字"
        state.temperature.toDouble() < 0.0 -> "temperature 不能小于 0"
        else -> null
    },
    maxIterations = when {
        state.maxIterations.isBlank() -> "请输入 max iterations"
        state.maxIterations.toIntOrNull() == null -> "max iterations 必须是整数"
        state.maxIterations.toInt() <= 0 -> "max iterations 必须大于 0"
        else -> null
    },
)

internal fun nativeSettingsSummary(errors: NativeFormErrors): String = buildList {
    errors.provider?.let(::add)
    errors.modelId?.let(::add)
    errors.apiKey?.let(::add)
    errors.baseUrl?.let(::add)
    errors.extraConfig?.let(::add)
    errors.temperature?.let(::add)
    errors.maxIterations?.let(::add)
}.joinToString("；")

internal fun NativeSettingsState.toStoredSettings(): StoredSettings = StoredSettings(
    providerName = provider.name,
    apiKey = apiKey,
    modelId = modelId,
    baseUrl = baseUrl,
    extraConfig = extraConfig,
    promptDraft = promptDraft,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxIterations = maxIterations,
)

internal fun NativeSettingsState.toAgentRequest(userPrompt: String): AgentRequest = AgentRequest(
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

