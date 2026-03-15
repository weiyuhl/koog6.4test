package com.lhzkml.codestudio.ui.model

import com.lhzkml.codestudio.FormErrors
import com.lhzkml.codestudio.Preset
import com.lhzkml.codestudio.Provider

// 设置主页 UI Model
internal data class SettingsHomeUiModel(
    val providerDisplayName: String,
    val runtimePresetTitle: String,
    val availableProviders: List<ProviderUiModel>,
    val availablePresets: List<PresetUiModel>,
    val configuredProviders: Set<String>,
    val errors: FormErrors
)

// Provider UI Model
internal data class ProviderUiModel(
    val name: String,
    val displayName: String,
    val isSupported: Boolean
)

// Preset UI Model
internal data class PresetUiModel(
    val id: String,
    val title: String,
    val description: String
)

// Provider 设置页 UI Model
internal data class ProviderSettingsUiModel(
    val providerDisplayName: String,
    val modelId: String,
    val modelIdPlaceholder: String,
    val apiKey: String,
    val apiKeyPlaceholder: String,
    val showApiKey: Boolean,
    val baseUrl: String,
    val baseUrlLabel: String,
    val baseUrlPlaceholder: String,
    val showBaseUrl: Boolean,
    val extraConfig: String,
    val extraFieldLabel: String?,
    val extraFieldPlaceholder: String,
    val showExtraField: Boolean,
    val supportsBalanceCheck: Boolean,
    val balanceInfo: BalanceDisplayInfo?,
    val isCheckingBalance: Boolean,
    val errors: FormErrors
)

// 余额显示信息
internal data class BalanceDisplayInfo(
    val isAvailable: Boolean,
    val currency: String,
    val totalBalance: String,
    val grantedBalance: String?,
    val toppedUpBalance: String?,
    val errorMessage: String?
)

// Runtime 设置页 UI Model
internal data class RuntimeSettingsUiModel(
    val runtimePresetTitle: String,
    val systemPrompt: String,
    val systemPromptPlaceholder: String,
    val temperature: String,
    val temperaturePlaceholder: String,
    val maxIterations: String,
    val maxIterationsPlaceholder: String,
    val errors: FormErrors
)

// 转换函数
internal fun SettingsUiModel.toHomeUiModel(errors: FormErrors): SettingsHomeUiModel {
    return SettingsHomeUiModel(
        providerDisplayName = providerDisplayName,
        runtimePresetTitle = runtimePresetTitle,
        availableProviders = Provider.entries
            .filter { it.isSupportedOnAndroid }
            .map { ProviderUiModel(it.name, it.displayName, it.isSupportedOnAndroid) },
        availablePresets = Preset.entries
            .map { PresetUiModel(it.id, it.title, it.description) },
        configuredProviders = configuredProviders,
        errors = errors
    )
}

internal fun SettingsUiModel.toProviderUiModel(errors: FormErrors, balanceInfo: BalanceDisplayInfo? = null, isCheckingBalance: Boolean = false): ProviderSettingsUiModel {
    // 只有 DeepSeek 和 SiliconFlow 支持余额查询
    val supportsBalance = providerDisplayName in listOf("DeepSeek", "硅基流动")
    
    return ProviderSettingsUiModel(
        providerDisplayName = providerDisplayName,
        modelId = modelId,
        modelIdPlaceholder = defaultModelId,
        apiKey = apiKey,
        apiKeyPlaceholder = "输入 API 密钥",
        showApiKey = requiresApiKey,
        baseUrl = baseUrl,
        baseUrlLabel = baseUrlLabel,
        baseUrlPlaceholder = if (baseUrl.isBlank()) "https://api.example.com" else baseUrl,
        showBaseUrl = requiresBaseUrl,
        extraConfig = extraConfig,
        extraFieldLabel = extraFieldLabel,
        extraFieldPlaceholder = extraFieldDefault,
        showExtraField = requiresExtraConfig,
        supportsBalanceCheck = supportsBalance,
        balanceInfo = balanceInfo,
        isCheckingBalance = isCheckingBalance,
        errors = errors
    )
}

internal fun SettingsUiModel.toRuntimeUiModel(errors: FormErrors): RuntimeSettingsUiModel {
    return RuntimeSettingsUiModel(
        runtimePresetTitle = runtimePresetTitle,
        systemPrompt = systemPrompt,
        systemPromptPlaceholder = "可选的系统提示词",
        temperature = temperature,
        temperaturePlaceholder = "0.2",
        maxIterations = maxIterations,
        maxIterationsPlaceholder = "50",
        errors = errors
    )
}
