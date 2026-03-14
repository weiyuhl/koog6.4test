package com.lhzkml.codestudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.repository.ChatRepository
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.usecase.SendMessageUseCase
import com.lhzkml.codestudio.usecase.toSendMessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isRunning: Boolean = false,
    val provider: Provider = Provider.OPENAI
)

internal sealed interface ChatEvent {
    data class UpdatePrompt(val text: String) : ChatEvent
    data object SendMessage : ChatEvent
    data object ClearChat : ChatEvent
}

@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var nextMessageId = 0L
    
    init {
        loadMessages()
        observeSettings()
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            val messages = chatRepository.loadMessages()
            nextMessageId = (messages.maxOfOrNull { it.id } ?: 0L) + 1L
            _uiState.update { it.copy(messages = messages) }
        }
    }
    
    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val provider = Provider.entries.firstOrNull { 
                    it.name == settings.providerName 
                } ?: Provider.OPENAI
                _uiState.update { it.copy(provider = provider) }
            }
        }
    }
    
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.UpdatePrompt -> updatePrompt(event.text)
            is ChatEvent.SendMessage -> sendMessage()
            is ChatEvent.ClearChat -> clearChat()
        }
    }
    
    private fun updatePrompt(text: String) {
        _uiState.update { it.copy(prompt = text) }
    }
    
    private fun sendMessage() {
        val currentState = _uiState.value
        val userPrompt = currentState.prompt.trim()
        
        if (userPrompt.isBlank() || currentState.isRunning) return
        
        addMessage(MessageRole.User, userPrompt)
        val assistantId = addMessage(MessageRole.Assistant, STREAMING_PLACEHOLDER, currentState.provider.displayName, isStreaming = true)
        
        _uiState.update { it.copy(prompt = "", isRunning = true) }
        
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val presetId = settingsRepository.presetIdFlow.first()
            val preset = Preset.fromId(presetId)
            
            val state = State(
                provider = currentState.provider,
                apiKey = settings.apiKey,
                modelId = settings.modelId,
                baseUrl = settings.baseUrl,
                extraConfig = settings.extraConfig,
                runtimePreset = preset,
                systemPrompt = settings.systemPrompt,
                temperature = settings.temperature,
                maxIterations = settings.maxIterations
            )
            
            val validation = validateSettings(state)
            if (validation.hasAny()) {
                removeMessage(assistantId)
                addMessage(
                    MessageRole.System,
                    "当前还不能发送消息，请先到设置页完善配置：${settingsSummary(validation)}",
                    "设置未完成"
                )
                _uiState.update { it.copy(isRunning = false) }
                return@launch
            }
            
            val result = sendMessageUseCase.execute(
                request = state.toSendMessageRequest(userPrompt),
                onTextDelta = { delta ->
                    updateMessage(assistantId) { current ->
                        current.copy(
                            text = if (current.text == STREAMING_PLACEHOLDER) delta else current.text + delta,
                            isStreaming = true
                        )
                    }
                }
            )
            
            result.fold(
                onSuccess = { sendResult ->
                    updateMessage(assistantId) { current ->
                        current.copy(
                            text = current.text.takeUnless { it.isBlank() || it == STREAMING_PLACEHOLDER }
                                ?: sendResult.answer,
                            isStreaming = false
                        )
                    }
                    if (sendResult.hasEvents) {
                        addMessage(MessageRole.System, sendResult.events.joinToString("\n"), "执行日志")
                    }
                },
                onFailure = { error ->
                    removeMessage(assistantId)
                    addMessage(
                        MessageRole.System,
                        error.message ?: error::class.simpleName ?: "Unknown error",
                        "错误"
                    )
                }
            )
            
            _uiState.update { it.copy(isRunning = false) }
        }
    }
    
    private fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
        persistMessages()
        addMessage(MessageRole.System, "对话已清空。现在可以重新开始聊天", "新对话")
    }
    
    private fun addMessage(role: MessageRole, text: String, label: String? = null, isStreaming: Boolean = false): Long {
        val message = ChatMessage(nextMessageId++, role, text, label, isStreaming)
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
    
    private fun persistMessages() {
        viewModelScope.launch {
            chatRepository.saveMessages(_uiState.value.messages)
        }
    }
}
