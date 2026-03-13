package com.lhzkml.codestudio.validation

import com.lhzkml.codestudio.*

/**
 * 统一的校验服务
 * 所有校验逻辑集中在这里，避免分散在多个地方
 */
internal object ValidationService {
    
    /**
     * 校验完整的设置状态
     */
    fun validateSettings(state: State): FormErrors = FormErrors(
        provider = validateProvider(state.provider),
        modelId = validateModelId(state.modelId),
        apiKey = validateApiKey(state.provider, state.apiKey),
        baseUrl = validateBaseUrl(state.provider, state.baseUrl),
        extraConfig = validateExtraConfig(state.provider, state.extraConfig),
        temperature = validateTemperature(state.temperature),
        maxIterations = validateMaxIterations(state.maxIterations),
    )
    
    /**
     * 校验发送消息请求
     */
    fun validateSendMessageRequest(
        userPrompt: String,
        provider: Provider,
        apiKey: String,
        modelId: String,
        baseUrl: String
    ): String? {
        return when {
            userPrompt.isBlank() -> "消息不能为空"
            validateProvider(provider) != null -> "当前供应商不支持"
            validateModelId(modelId) != null -> "模型 ID 不能为空"
            validateApiKey(provider, apiKey) != null -> "API Key 不能为空"
            validateBaseUrl(provider, baseUrl) != null -> "Base URL 不能为空"
            else -> null
        }
    }
    
    private fun validateProvider(provider: Provider): String? {
        return if (!provider.isSupportedOnAndroid) {
            "当前 Android 版本暂不支持该供应商，请切换到其他供应商"
        } else null
    }
    
    private fun validateModelId(modelId: String): String? {
        return if (modelId.isBlank()) "请输入模型 ID" else null
    }
    
    private fun validateApiKey(provider: Provider, apiKey: String): String? {
        return if (provider.requiresApiKey && apiKey.isBlank()) {
            "请先输入 API Key"
        } else null
    }
    
    private fun validateBaseUrl(provider: Provider, baseUrl: String): String? {
        return if ((provider == Provider.AZURE_OPENAI || provider == Provider.OLLAMA) && baseUrl.isBlank()) {
            "该供应商需要 Base URL"
        } else null
    }
    
    private fun validateExtraConfig(provider: Provider, extraConfig: String): String? {
        return if (provider.extraFieldLabel != null && extraConfig.isBlank()) {
            "请填写该供应商所需的额外配置"
        } else null
    }
    
    private fun validateTemperature(temperature: String): String? {
        return when {
            temperature.isBlank() -> "请输入 temperature"
            temperature.toDoubleOrNull() == null -> "temperature 必须是数字"
            temperature.toDouble() < 0.0 -> "temperature 不能小于 0"
            else -> null
        }
    }
    
    private fun validateMaxIterations(maxIterations: String): String? {
        return when {
            maxIterations.isBlank() -> "请输入 max iterations"
            maxIterations.toIntOrNull() == null -> "max iterations 必须是整数"
            maxIterations.toInt() <= 0 -> "max iterations 必须大于 0"
            else -> null
        }
    }
}
