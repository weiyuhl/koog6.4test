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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
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
    val formErrors: FormErrors = FormErrors(),
    val openRouterKeyInfo: OpenRouterKeyInfo? = null,
    val isLoadingKeyInfo: Boolean = false,
    val availableModels: List<OpenRouterModelInfo> = emptyList(),
    val isLoadingModels: Boolean = false,
    val modelSearchQuery: String = "",
    val modelFilterFree: Boolean? = null,
    val modelFilterInputModalities: Set<String> = emptySet(),
    val modelFilterOutputModalities: Set<String> = emptySet(),
    val modelSortBy: ModelSortOption = ModelSortOption.NEWEST
)

internal data class OpenRouterKeyInfo(
    val label: String?,
    val usage: Double?,
    val usageDaily: Double?,
    val usageWeekly: Double?,
    val usageMonthly: Double?,
    val limit: Double?,
    val limitRemaining: Double?,
    val limitReset: String?,
    val isFree: Boolean,
    val isManagementKey: Boolean,
    val byokUsage: Double?,
    val rateLimit: String?,
    val expiresAt: String?,
    val errorMessage: String?
)

internal data class OpenRouterModelInfo(
    val id: String,
    val name: String?,
    val contextLength: Int?,
    val maxOutputTokens: Int?,
    val promptPrice: String?,
    val completionPrice: String?,
    val description: String?,
    val supportedParameters: List<String>?,
    val inputModalities: List<String>?,
    val outputModalities: List<String>?,
    val topProvider: String?
)

internal enum class ModelSortOption {
    NEWEST,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    CONTEXT_HIGH_TO_LOW,
    CONTEXT_LOW_TO_HIGH
}

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
    data object LoadOpenRouterKeyInfo : SettingsEvent
    data object LoadAvailableModels : SettingsEvent
    data class UpdateModelSearchQuery(val query: String) : SettingsEvent
    data class UpdateModelFilterFree(val freeOnly: Boolean?) : SettingsEvent
    data class ToggleInputModality(val modality: String) : SettingsEvent
    data class ToggleOutputModality(val modality: String) : SettingsEvent
    data class UpdateModelSortBy(val sortBy: ModelSortOption) : SettingsEvent
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
            is SettingsEvent.LoadOpenRouterKeyInfo -> loadOpenRouterKeyInfo()
            is SettingsEvent.LoadAvailableModels -> loadAvailableModels()
            is SettingsEvent.UpdateModelSearchQuery -> updateModelSearchQuery(event.query)
            is SettingsEvent.UpdateModelFilterFree -> updateModelFilterFree(event.freeOnly)
            is SettingsEvent.ToggleInputModality -> toggleInputModality(event.modality)
            is SettingsEvent.ToggleOutputModality -> toggleOutputModality(event.modality)
            is SettingsEvent.UpdateModelSortBy -> updateModelSortBy(event.sortBy)
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
    
    private fun loadOpenRouterKeyInfo() {
        viewModelScope.launch {
            val state = _uiState.value
            
            if (state.provider != Provider.OPENROUTER) {
                return@launch
            }
            
            if (state.apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        openRouterKeyInfo = OpenRouterKeyInfo(
                            label = null,
                            usage = null,
                            usageDaily = null,
                            usageWeekly = null,
                            usageMonthly = null,
                            limit = null,
                            limitRemaining = null,
                            limitReset = null,
                            isFree = false,
                            isManagementKey = false,
                            byokUsage = null,
                            rateLimit = null,
                            expiresAt = null,
                            errorMessage = "请先填写 API Key"
                        )
                    )
                }
                return@launch
            }
            
            _uiState.update { it.copy(isLoadingKeyInfo = true, openRouterKeyInfo = null) }
            
            try {
                val chatService = com.lhzkml.codestudio.service.ChatService()
                val client = chatService.createClient(
                    provider = state.provider,
                    apiKey = state.apiKey,
                    baseUrl = state.baseUrl,
                    extraConfig = state.extraConfig
                )
                
                val result = chatService.getKeyInfo(client)
                val keyInfo = when (result) {
                    is com.lhzkml.codestudio.service.ChatService.OpenRouterKeyInfoResult.Success -> {
                        val data = result.data.data
                        val rateLimitStr = data.rate_limit?.let { 
                            "${it.requests} 请求/${it.interval}" 
                        }
                        OpenRouterKeyInfo(
                            label = data.label,
                            usage = data.usage,
                            usageDaily = data.usage_daily,
                            usageWeekly = data.usage_weekly,
                            usageMonthly = data.usage_monthly,
                            limit = data.limit,
                            limitRemaining = data.limit_remaining,
                            limitReset = data.limit_reset,
                            isFree = data.is_free_tier ?: false,
                            isManagementKey = data.is_management_key ?: false,
                            byokUsage = data.byok_usage,
                            rateLimit = rateLimitStr,
                            expiresAt = data.expires_at,
                            errorMessage = null
                        )
                    }
                    is com.lhzkml.codestudio.service.ChatService.OpenRouterKeyInfoResult.Error -> 
                        OpenRouterKeyInfo(
                            label = null,
                            usage = null,
                            usageDaily = null,
                            usageWeekly = null,
                            usageMonthly = null,
                            limit = null,
                            limitRemaining = null,
                            limitReset = null,
                            isFree = false,
                            isManagementKey = false,
                            byokUsage = null,
                            rateLimit = null,
                            expiresAt = null,
                            errorMessage = result.message
                        )
                    is com.lhzkml.codestudio.service.ChatService.OpenRouterKeyInfoResult.NotSupported -> 
                        OpenRouterKeyInfo(
                            label = null,
                            usage = null,
                            usageDaily = null,
                            usageWeekly = null,
                            usageMonthly = null,
                            limit = null,
                            limitRemaining = null,
                            limitReset = null,
                            isFree = false,
                            isManagementKey = false,
                            byokUsage = null,
                            rateLimit = null,
                            expiresAt = null,
                            errorMessage = "该供应商不支持此功能"
                        )
                }
                
                _uiState.update { it.copy(openRouterKeyInfo = keyInfo, isLoadingKeyInfo = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        openRouterKeyInfo = OpenRouterKeyInfo(
                            label = null,
                            usage = null,
                            usageDaily = null,
                            usageWeekly = null,
                            usageMonthly = null,
                            limit = null,
                            limitRemaining = null,
                            limitReset = null,
                            isFree = false,
                            isManagementKey = false,
                            byokUsage = null,
                            rateLimit = null,
                            expiresAt = null,
                            errorMessage = "查询失败: ${e.message ?: "未知错误"}"
                        ),
                        isLoadingKeyInfo = false
                    )
                }
            }
        }
    }
    
    private fun loadAvailableModels() {
        viewModelScope.launch {
            val state = _uiState.value
            
            if (state.apiKey.isBlank()) {
                return@launch
            }
            
            _uiState.update { it.copy(isLoadingModels = true, availableModels = emptyList()) }
            
            try {
                val chatService = com.lhzkml.codestudio.service.ChatService()
                val client = chatService.createClient(
                    provider = state.provider,
                    apiKey = state.apiKey,
                    baseUrl = state.baseUrl,
                    extraConfig = state.extraConfig
                )
                
                if (client is com.lhzkml.jasmine.core.prompt.executor.OpenRouterClient) {
                    val rawJson = client.listModelsRaw()
                    val modelInfos = parseOpenRouterModels(rawJson)
                    _uiState.update { it.copy(availableModels = modelInfos, isLoadingModels = false) }
                } else {
                    val models = client.listModels()
                    val modelInfos = models.map { model ->
                        OpenRouterModelInfo(
                            id = model.id,
                            name = model.displayName,
                            contextLength = model.contextLength,
                            maxOutputTokens = model.maxOutputTokens,
                            promptPrice = null,
                            completionPrice = null,
                            description = model.description,
                            supportedParameters = null,
                            inputModalities = null,
                            outputModalities = null,
                            topProvider = null
                        )
                    }
                    _uiState.update { it.copy(availableModels = modelInfos, isLoadingModels = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(availableModels = emptyList(), isLoadingModels = false) }
            }
        }
    }
    
    private fun parseOpenRouterModels(jsonString: String): List<OpenRouterModelInfo> {
        return try {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(jsonString).jsonObject
            val dataArray = root["data"]?.jsonArray ?: return emptyList()
            
            dataArray.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull
                    val description = obj["description"]?.jsonPrimitive?.contentOrNull
                    val contextLength = obj["context_length"]?.jsonPrimitive?.intOrNull
                    
                    val pricing = obj["pricing"]?.jsonObject
                    val promptPrice = pricing?.get("prompt")?.jsonPrimitive?.contentOrNull
                    val completionPrice = pricing?.get("completion")?.jsonPrimitive?.contentOrNull
                    
                    val topProvider = obj["top_provider"]?.jsonObject
                    val maxCompletionTokens = topProvider?.get("max_completion_tokens")?.jsonPrimitive?.intOrNull
                    
                    val supportedParams = obj["supported_parameters"]?.jsonArray?.mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    }
                    
                    val architecture = obj["architecture"]?.jsonObject
                    val inputModalities = architecture?.get("input_modalities")?.jsonArray?.mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    }
                    val outputModalities = architecture?.get("output_modalities")?.jsonArray?.mapNotNull {
                        it.jsonPrimitive.contentOrNull
                    }
                    
                    val topProviderName = id.substringBefore("/")
                    
                    OpenRouterModelInfo(
                        id = id,
                        name = name,
                        contextLength = contextLength,
                        maxOutputTokens = maxCompletionTokens,
                        promptPrice = promptPrice,
                        completionPrice = completionPrice,
                        description = description,
                        supportedParameters = supportedParams,
                        inputModalities = inputModalities,
                        outputModalities = outputModalities,
                        topProvider = topProviderName
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun updateModelSearchQuery(query: String) {
        _uiState.update { it.copy(modelSearchQuery = query) }
    }
    
    private fun updateModelFilterFree(freeOnly: Boolean?) {
        _uiState.update { it.copy(modelFilterFree = freeOnly) }
    }
    
    private fun toggleInputModality(modality: String) {
        _uiState.update { state ->
            val newSet = if (state.modelFilterInputModalities.contains(modality)) {
                state.modelFilterInputModalities - modality
            } else {
                state.modelFilterInputModalities + modality
            }
            state.copy(modelFilterInputModalities = newSet)
        }
    }
    
    private fun toggleOutputModality(modality: String) {
        _uiState.update { state ->
            val newSet = if (state.modelFilterOutputModalities.contains(modality)) {
                state.modelFilterOutputModalities - modality
            } else {
                state.modelFilterOutputModalities + modality
            }
            state.copy(modelFilterOutputModalities = newSet)
        }
    }
    
    private fun updateModelSortBy(sortBy: ModelSortOption) {
        _uiState.update { it.copy(modelSortBy = sortBy) }
    }
    
    fun getFilteredAndSortedModels(): List<OpenRouterModelInfo> {
        val state = _uiState.value
        var filtered = state.availableModels
        
        // 搜索筛选
        if (state.modelSearchQuery.isNotBlank()) {
            val query = state.modelSearchQuery.lowercase()
            filtered = filtered.filter { model ->
                model.id.lowercase().contains(query) ||
                model.name?.lowercase()?.contains(query) == true ||
                model.description?.lowercase()?.contains(query) == true
            }
        }
        
        // 价格筛选（免费/付费）
        state.modelFilterFree?.let { freeOnly ->
            filtered = filtered.filter { model ->
                val isFree = model.promptPrice == "0" || model.promptPrice == null
                if (freeOnly) isFree else !isFree
            }
        }
        
        // 输入模态筛选
        if (state.modelFilterInputModalities.isNotEmpty()) {
            filtered = filtered.filter { model ->
                model.inputModalities?.any { it in state.modelFilterInputModalities } == true
            }
        }
        
        // 输出模态筛选
        if (state.modelFilterOutputModalities.isNotEmpty()) {
            filtered = filtered.filter { model ->
                model.outputModalities?.any { it in state.modelFilterOutputModalities } == true
            }
        }
        
        // 排序
        return when (state.modelSortBy) {
            ModelSortOption.NEWEST -> filtered
            ModelSortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { 
                it.promptPrice?.toDoubleOrNull() ?: Double.MAX_VALUE 
            }
            ModelSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { 
                it.promptPrice?.toDoubleOrNull() ?: 0.0 
            }
            ModelSortOption.CONTEXT_HIGH_TO_LOW -> filtered.sortedByDescending { 
                it.contextLength ?: 0 
            }
            ModelSortOption.CONTEXT_LOW_TO_HIGH -> filtered.sortedBy { 
                it.contextLength ?: Int.MAX_VALUE 
            }
        }
    }
    
    fun toUiModel(): SettingsUiModel = _uiState.value.toUiModel()
}
