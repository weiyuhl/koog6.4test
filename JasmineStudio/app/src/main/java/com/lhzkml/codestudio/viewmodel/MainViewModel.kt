package com.lhzkml.codestudio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.codestudio.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class MainUiState(
    val currentRoute: String = Route.Chat.value,
    val provider: Provider = Provider.OPENAI,
    val apiKey: String = "",
    val modelId: String = "",
    val baseUrl: String = "",
    val extraConfig: String = "",
    val prompt: String = "",
    val runtimePreset: Preset = Preset.GraphToolsSequential,
    val systemPrompt: String = "",
    val temperature: String = "0.2",
    val maxIterations: String = "50",
    val formErrors: FormErrors = FormErrors(),
    val isRunning: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val isSidebarOpen: Boolean = false
)

internal class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val localStore = LocalStore(application)
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private var nextMessageId = 0L
    
    init {
        loadInitialState()
    }
    
    private fun loadInitialState() {
        val restoredState = localStore.loadState()
        val restoredProvider = Provider.entries.firstOrNull { 
            it.name == restoredState.settings.providerName 
        } ?: Provider.OPENAI
        val restoredPreset = Preset.fromId(localStore.loadRuntimePresetId())
        val restoredMessages = restoredState.messages.map { it.toChatMessage() }
        
        nextMessageId = (restoredMessages.maxOfOrNull { it.id } ?: 0L) + 1L
        
        _uiState.update {
            it.copy(
                provider = restoredProvider,
                apiKey = restoredState.settings.apiKey,
                modelId = restoredState.settings.modelId,
                baseUrl = restoredState.settings.baseUrl,
                extraConfig = restoredState.settings.extraConfig,
                prompt = restoredState.settings.promptDraft,
                runtimePreset = restoredPreset,
                systemPrompt = restoredState.settings.systemPrompt,
                temperature = restoredState.settings.temperature,
                maxIterations = restoredState.settings.maxIterations,
                messages = restoredMessages
            )
        }
    }
    
    fun navigateTo(route: String) {
        _uiState.update { it.copy(currentRoute = route) }
    }
    
    fun openSidebar() {
        _uiState.update { it.copy(isSidebarOpen = true) }
    }
    
    fun closeSidebar() {
        _uiState.update { it.copy(isSidebarOpen = false) }
    }
    
    fun updateProvider(provider: Provider) {
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
    
    fun updateApiKey(value: String) {
        _uiState.update {
            it.copy(
                apiKey = value,
                formErrors = it.formErrors.copy(apiKey = null)
            )
        }
        persistSettings()
    }
    
    fun updateModelId(value: String) {
        _uiState.update {
            it.copy(
                modelId = value,
                formErrors = it.formErrors.copy(modelId = null)
            )
        }
        persistSettings()
    }
    
    fun updateBaseUrl(value: String) {
        _uiState.update {
            it.copy(
                baseUrl = value,
                formErrors = it.formErrors.copy(baseUrl = null)
            )
        }
        persistSettings()
    }
    
    fun updateExtraConfig(value: String) {
        _uiState.update {
            it.copy(
                extraConfig = value,
                formErrors = it.formErrors.copy(extraConfig = null)
            )
        }
        persistSettings()
    }
    
    fun updatePrompt(value: String) {
        _uiState.update { it.copy(prompt = value) }
        persistSettings()
    }
    
    fun updateRuntimePreset(preset: Preset) {
        _uiState.update { it.copy(runtimePreset = preset) }
        persistSettings()
    }
    
    fun updateSystemPrompt(value: String) {
        _uiState.update { it.copy(systemPrompt = value) }
        persistSettings()
    }
    
    fun updateTemperature(value: String) {
        _uiState.update {
            it.copy(
                temperature = value,
                formErrors = it.formErrors.copy(temperature = null)
            )
        }
        persistSettings()
    }
    
    fun updateMaxIterations(value: String) {
        _uiState.update {
            it.copy(
                maxIterations = value,
                formErrors = it.formErrors.copy(maxIterations = null)
            )
        }
        persistSettings()
    }

    
    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
        persistMessages()
        addMessage(MessageRole.System, "对话已清空。现在可以重新开始聊天", "新对话")
    }
    
    fun submitPrompt() {
        val currentState = _uiState.value
        val userPrompt = currentState.prompt.trim()
        
        if (userPrompt.isBlank() || currentState.isRunning) return
        
        val validation = validateSettings(currentState.toState())
        _uiState.update { it.copy(formErrors = validation) }
        
        if (validation.hasAny()) {
            addMessage(
                MessageRole.System,
                "当前还不能发送消息，请先到设置页完善配置：${settingsSummary(validation)}",
                "设置未完成"
            )
            navigateTo(Route.Home.value)
            return
        }
        
        addMessage(MessageRole.User, userPrompt)
        val assistantId = addMessage(MessageRole.Assistant, STREAMING_PLACEHOLDER, currentState.provider.displayName)
        
        _uiState.update { it.copy(prompt = "", isRunning = true) }
        persistSettings()
        
        viewModelScope.launch {
            try {
                val result = AgentRunner.runAgentStreaming(
                    request = currentState.toState().toAgentRequest(userPrompt),
                    onTextDelta = { delta ->
                        updateMessage(assistantId) { current ->
                            current.copy(
                                text = if (current.text == STREAMING_PLACEHOLDER) delta else current.text + delta
                            )
                        }
                    }
                )
                updateMessage(assistantId) { current ->
                    current.copy(
                        text = current.text.takeUnless { it.isBlank() || it == STREAMING_PLACEHOLDER }
                            ?: result.answer
                    )
                }
                if (result.events.isNotEmpty()) {
                    addMessage(MessageRole.System, result.events.joinToString("\n"), "执行日志")
                }
            } catch (error: Throwable) {
                removeMessage(assistantId)
                addMessage(
                    MessageRole.System,
                    error.message ?: error::class.simpleName ?: "Unknown error",
                    "错误"
                )
            } finally {
                _uiState.update { it.copy(isRunning = false) }
            }
        }
    }
    
    private fun addMessage(role: MessageRole, text: String, label: String? = null): Long {
        val message = ChatMessage(nextMessageId++, role, text, label)
        _uiState.update { it.copy(messages = it.messages + message) }
        persistMessages()
        return message.id
    }
    
    private fun updateMessage(id: Long, update: (ChatMessage) -> ChatMessage) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { if (it.id == id) update(it) else it }
            )
        }
        persistMessages()
    }
    
    private fun removeMessage(id: Long) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filter { it.id != id })
        }
        persistMessages()
    }
    
    private fun persistSettings() {
        val state = _uiState.value.toState()
        localStore.saveSettings(state.toStoredSettings())
        localStore.saveRuntimePresetId(state.runtimePreset.id)
    }
    
    private fun persistMessages() {
        localStore.saveMessages(_uiState.value.messages.map(ChatMessage::toStoredMessage))
    }
    
    private fun MainUiState.toState(): State = State(
        provider = provider,
        apiKey = apiKey,
        modelId = modelId,
        baseUrl = baseUrl,
        extraConfig = extraConfig,
        promptDraft = prompt,
        runtimePreset = runtimePreset,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxIterations = maxIterations
    )
}
