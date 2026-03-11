package com.lhzkml.codestudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.ui.model.SettingsUiModel
import com.lhzkml.codestudio.ui.model.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
}

internal class SettingsViewModel(
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
        }
    }
    
    private fun updateProvider(provider: Provider) {
        _uiState.update {
            it.copy(
                provider = provider,
                modelId = provider.defaultModelId,
                baseUrl = provider.defaultBaseUrl,
                extraConfig = provider.extraFieldDefault,
                formErrors = FormErrors()
            )
        }
        persistSettings()
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
        settingsRepository.updatePresetId(preset.id)
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
    
    fun toUiModel(): SettingsUiModel = _uiState.value.toUiModel()
}
