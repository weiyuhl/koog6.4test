package com.lhzkml.codestudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.repository.StoredSettings
import com.lhzkml.codestudio.ui.model.SettingsUiModel
import com.lhzkml.codestudio.ui.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SettingsUiState(
    val provider: Provider = Provider.OPENAI,
    val apiKey: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val extraConfig: String = "",
    val runtimePreset: Preset = Preset.GraphToolsSequential,
    val systemPrompt: String = "",
    val temperature: String = "0.2",
    val maxIterations: String = "50",
    val balanceInfo: com.lhzkml.codestudio.ui.model.BalanceDisplayInfo? = null,
    val isCheckingBalance: Boolean = false,
    val formErrors: FormErrors = FormErrors()
)

internal sealed interface SettingsEvent {
    data class UpdateProvider(val provider: Provider) : SettingsEvent
    data class UpdateApiKey(val value: String) : SettingsEvent
    data class UpdateModelId(val value: String) : SettingsEvent
    data class UpdateBaseUrl(val value: String) : SettingsEvent
    data class UpdateExtraConfig(val value: String) : SettingsEvent
    data class UpdateRuntimePreset(val preset: Preset) : SettingsEvent
    data class UpdateSystemPrompt(val value: String) : SettingsEvent
    data class UpdateTemperature(val value: String) : SettingsEvent
    data class UpdateMaxIterations(val value: String) : SettingsEvent
    data object CheckBalance : SettingsEvent
}

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeSettings()
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val provider = Provider.entries.firstOrNull { 
                    it.name == settings.providerName 
                } ?: Provider.OPENAI
                
                _uiState.update {
                    it.copy(
                        provider = provider,
                        apiKey = settings.apiKey,
                        modelId = settings.modelId,
                        baseUrl = settings.baseUrl,
                        extraConfig = settings.extraConfig,
                        systemPrompt = settings.systemPrompt,
                        temperature = settings.temperature,
                        maxIterations = settings.maxIterations
                    )
                }
            }
        }
        
        viewModelScope.launch {
            settingsRepository.presetIdFlow.collect { presetId ->
                val preset = Preset.fromId(presetId)
                _uiState.update { it.copy(runtimePreset = preset) }
            }
        }
    }
    
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateProvider -> updateProvider(event.provider)
            is SettingsEvent.UpdateApiKey -> updateApiKey(event.value)
            is SettingsEvent.UpdateModelId -> updateModelId(event.value)
            is SettingsEvent.UpdateBaseUrl -> updateBaseUrl(event.value)
            is SettingsEvent.UpdateExtraConfig -> updateExtraConfig(event.value)
            is SettingsEvent.UpdateRuntimePreset -> updateRuntimePreset(event.preset)
            is SettingsEvent.UpdateSystemPrompt -> updateSystemPrompt(event.value)
            is SettingsEvent.UpdateTemperature -> updateTemperature(event.value)
            is SettingsEvent.UpdateMaxIterations -> updateMaxIterations(event.value)
            is SettingsEvent.CheckBalance -> checkBalance()
        }
    }
    
    private fun updateProvider(provider: Provider) {
        viewModelScope.launch {
            // 切换供应商
            settingsRepository.updateCurrentProvider(provider.name)
            
            // 清空余额信息
            _uiState.update {
                it.copy(
                    provider = provider,
                    balanceInfo = null,
                    formErrors = FormErrors()
                )
            }
        }
    }
    
    private fun updateApiKey(value: String) {
        _uiState.update {
            it.copy(
                apiKey = value,
                formErrors = it.formErrors.copy(apiKey = null)
            )
        }
        persistSettings()
    }
    
    private fun updateModelId(value: String) {
        _uiState.update {
            it.copy(
                modelId = value,
                formErrors = it.formErrors.copy(modelId = null)
            )
        }
        persistSettings()
    }
    
    private fun updateBaseUrl(value: String) {
        _uiState.update {
            it.copy(
                baseUrl = value,
                formErrors = it.formErrors.copy(baseUrl = null)
            )
        }
        persistSettings()
    }
    
    private fun updateExtraConfig(value: String) {
        _uiState.update {
            it.copy(
                extraConfig = value,
                formErrors = it.formErrors.copy(extraConfig = null)
            )
        }
        persistSettings()
    }
    
    private fun updateRuntimePreset(preset: Preset) {
        _uiState.update { it.copy(runtimePreset = preset) }
        viewModelScope.launch {
            settingsRepository.updatePresetId(preset.id)
        }
    }
    
    private fun updateSystemPrompt(value: String) {
        _uiState.update { it.copy(systemPrompt = value) }
        persistSettings()
    }
    
    private fun updateTemperature(value: String) {
        _uiState.update {
            it.copy(
                temperature = value,
                formErrors = it.formErrors.copy(temperature = null)
            )
        }
        persistSettings()
    }
    
    private fun updateMaxIterations(value: String) {
        _uiState.update {
            it.copy(
                maxIterations = value,
                formErrors = it.formErrors.copy(maxIterations = null)
            )
        }
        persistSettings()
    }
    
    private fun persistSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // 执行校验并更新 formErrors
            val domainState = State(
                provider = state.provider,
                apiKey = state.apiKey,
                modelId = state.modelId,
                baseUrl = state.baseUrl,
                extraConfig = state.extraConfig,
                runtimePreset = state.runtimePreset,
                systemPrompt = state.systemPrompt,
                temperature = state.temperature,
                maxIterations = state.maxIterations
            )
            val errors = validateSettings(domainState)
            _uiState.update { it.copy(formErrors = errors) }
            
            settingsRepository.updateSettings(
                StoredSettings(
                    providerName = state.provider.name,
                    apiKey = state.apiKey,
                    modelId = state.modelId,
                    baseUrl = state.baseUrl,
                    extraConfig = state.extraConfig,
                    systemPrompt = state.systemPrompt,
                    temperature = state.temperature,
                    maxIterations = state.maxIterations
                )
            )
        }
    }
    
    private fun checkBalance() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // 检查 API Key 是否为空
            if (state.apiKey.isBlank()) {
                _uiState.update { 
                    it.copy(
                        balanceInfo = com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                            isAvailable = false,
                            currency = "",
                            totalBalance = "",
                            grantedBalance = null,
                            toppedUpBalance = null,
                            errorMessage = "请先填写 API Key"
                        )
                    ) 
                }
                return@launch
            }
            
            _uiState.update { it.copy(isCheckingBalance = true, balanceInfo = null) }
            
            try {
                val chatService = com.lhzkml.codestudio.service.ChatService()
                val client = chatService.createClient(
                    provider = state.provider,
                    apiKey = state.apiKey,
                    baseUrl = state.baseUrl,
                    extraConfig = state.extraConfig
                )
                
                val result = chatService.getBalance(client)
                val balanceInfo = when (result) {
                    is com.lhzkml.codestudio.service.ChatService.BalanceResult.Success -> {
                        // 解析 BalanceInfo
                        val balance = client.getBalance()
                        if (balance != null && balance.balances.isNotEmpty()) {
                            val detail = balance.balances.first()
                            com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                                isAvailable = balance.isAvailable,
                                currency = detail.currency,
                                totalBalance = detail.totalBalance,
                                grantedBalance = detail.grantedBalance,
                                toppedUpBalance = detail.toppedUpBalance,
                                errorMessage = null
                            )
                        } else {
                            com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                                isAvailable = false,
                                currency = "",
                                totalBalance = "",
                                grantedBalance = null,
                                toppedUpBalance = null,
                                errorMessage = "无法获取余额信息"
                            )
                        }
                    }
                    is com.lhzkml.codestudio.service.ChatService.BalanceResult.Error -> 
                        com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                            isAvailable = false,
                            currency = "",
                            totalBalance = "",
                            grantedBalance = null,
                            toppedUpBalance = null,
                            errorMessage = result.message
                        )
                    is com.lhzkml.codestudio.service.ChatService.BalanceResult.NotSupported -> 
                        com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                            isAvailable = false,
                            currency = "",
                            totalBalance = "",
                            grantedBalance = null,
                            toppedUpBalance = null,
                            errorMessage = "该供应商不支持余额查询"
                        )
                }
                
                _uiState.update { it.copy(balanceInfo = balanceInfo, isCheckingBalance = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        balanceInfo = com.lhzkml.codestudio.ui.model.BalanceDisplayInfo(
                            isAvailable = false,
                            currency = "",
                            totalBalance = "",
                            grantedBalance = null,
                            toppedUpBalance = null,
                            errorMessage = "查询失败: ${e.message ?: "未知错误"}"
                        ),
                        isCheckingBalance = false
                    ) 
                }
            }
        }
    }
    
    fun toUiModel(): SettingsUiModel = _uiState.value.toUiModel()
}
