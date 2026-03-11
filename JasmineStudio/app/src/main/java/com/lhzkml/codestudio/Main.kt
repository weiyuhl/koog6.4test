package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import com.lhzkml.codestudio.components.Icon
import com.lhzkml.codestudio.components.Side
import com.lhzkml.codestudio.components.SideContent
import com.lhzkml.codestudio.components.SideItem
import com.lhzkml.codestudio.components.Text
import com.lhzkml.codestudio.components.rememberSideState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun App() {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val sideState = rememberSideState(false)
    val localStore = remember(appContext) { LocalStore(appContext) }
    val restoredState = remember(localStore) { localStore.loadState() }
    val restoredProvider = remember(restoredState.settings.providerName) { 
        Provider.entries.firstOrNull { it.name == restoredState.settings.providerName } ?: Provider.OPENAI 
    }
    val restoredPreset = remember(localStore) { Preset.fromId(localStore.loadRuntimePresetId()) }
    val restoredMessages = remember(restoredState.messages) { restoredState.messages.map { it.toChatMessage() } }

    var currentRoute by rememberSaveable { mutableStateOf(Route.Chat.value) }
    var providerName by rememberSaveable { mutableStateOf(restoredProvider.name) }
    var apiKey by rememberSaveable { mutableStateOf(restoredState.settings.apiKey) }
    var modelId by rememberSaveable { mutableStateOf(restoredState.settings.modelId) }
    var baseUrl by rememberSaveable { mutableStateOf(restoredState.settings.baseUrl) }
    var extraConfig by rememberSaveable { mutableStateOf(restoredState.settings.extraConfig) }
    var prompt by rememberSaveable { mutableStateOf(restoredState.settings.promptDraft) }
    var runtimePreset by rememberSaveable { mutableStateOf(restoredPreset) }
    var systemPrompt by rememberSaveable { mutableStateOf(restoredState.settings.systemPrompt) }
    var temperature by rememberSaveable { mutableStateOf(restoredState.settings.temperature) }
    var maxIterations by rememberSaveable { mutableStateOf(restoredState.settings.maxIterations) }
    var formErrors by remember { mutableStateOf(FormErrors()) }
    var isRunning by remember { mutableStateOf(false) }
    var nextMessageId by remember { mutableLongStateOf((restoredMessages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(restoredMessages) } }
    val provider = remember(providerName) { Provider.valueOf(providerName) }

    fun currentState(
        providerValue: Provider = provider,
        apiKeyValue: String = apiKey,
        modelIdValue: String = modelId,
        baseUrlValue: String = baseUrl,
        extraConfigValue: String = extraConfig,
        promptValue: String = prompt,
        runtimePresetValue: Preset = runtimePreset,
        systemPromptValue: String = systemPrompt,
        temperatureValue: String = temperature,
        maxIterationsValue: String = maxIterations
    ) = State(
        providerValue,
        apiKeyValue,
        modelIdValue,
        baseUrlValue,
        extraConfigValue,
        promptValue,
        runtimePresetValue,
        systemPromptValue,
        temperatureValue,
        maxIterationsValue
    )

    fun persistSettings(state: State = currentState()) {
        localStore.saveSettings(state.toStoredSettings())
        localStore.saveRuntimePresetId(state.runtimePreset.id)
    }

    fun persistMessages() {
        localStore.saveMessages(messages.map(ChatMessage::toStoredMessage))
    }

    fun addMessage(role: MessageRole, text: String, label: String? = null): Long {
        val message = ChatMessage(nextMessageId++, role, text, label)
        messages += message
        persistMessages()
        return message.id
    }

    fun updateMessage(id: Long, update: (ChatMessage) -> ChatMessage) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages[index] = update(messages[index])
            persistMessages()
        }
    }

    fun removeMessage(id: Long) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages.removeAt(index)
            persistMessages()
        }
    }

    fun clearChat() {
        messages.clear()
        persistMessages()
        addMessage(MessageRole.System, "对话已清空。现在可以重新开始聊天", "新对话")
    }

    fun navigateTo(route: String) {
        currentRoute = route
    }

    fun openSettings() {
        scope.launch { sideState.close() }
        currentRoute = Route.Home.value
    }

    fun openChat() {
        scope.launch { sideState.close() }
        currentRoute = Route.Chat.value
    }

    fun submitPrompt() {
        val userPrompt = prompt.trim()
        if (userPrompt.isBlank() || isRunning) return
        val validation = validateSettings(currentState())
        formErrors = validation
        if (validation.hasAny()) {
            addMessage(
                MessageRole.System,
                "当前还不能发送消息，请先到设置页完善配置：${settingsSummary(validation)}",
                "设置未完成"
            )
            openSettings()
            return
        }
        addMessage(MessageRole.User, userPrompt)
        val assistantId = addMessage(MessageRole.Assistant, STREAMING_PLACEHOLDER, provider.displayName)
        prompt = ""
        persistSettings(currentState(promptValue = ""))
        isRunning = true
        scope.launch {
            try {
                val result = AgentRunner.runAgentStreaming(
                    request = currentState(promptValue = "").toAgentRequest(userPrompt),
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
                isRunning = false
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (sideState.isOpen) sideState.close()
    }

    Side(
        sideState = sideState,
        sideContent = {
            SideContent {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top section
                    Column {
                        Text(
                            "Chat",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(20.dp)
                        )

                        SideItem(
                            icon = { Text("💬") },
                            label = { Text("聊天", fontSize = 16.sp) },
                            selected = currentRoute == Route.Chat.value,
                            onClick = ::openChat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        SideItem(
                            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            label = { Text("清空对话", fontSize = 16.sp) },
                            selected = false,
                            onClick = {
                                clearChat()
                                scope.launch { sideState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Bottom section
                    Column(
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "消息: ${messages.size}",
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                            Text(
                                provider.displayName,
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                        }

                        SideItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("设置", fontSize = 16.sp) },
                            selected = currentRoute != Route.Chat.value,
                            onClick = ::openSettings,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
    ) {
        when (currentRoute) {
            Route.Chat.value -> {
                ChatScreen(
                    provider = provider,
                    prompt = prompt,
                    isRunning = isRunning,
                    messages = messages,
                    onPromptChanged = { prompt = it; persistSettings(currentState(promptValue = it)) },
                    onSendClick = ::submitPrompt,
                    onMenuClick = { scope.launch { sideState.open() } },
                )
            }

            Route.Home.value -> {
                SettingsHomeScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { currentRoute = Route.Chat.value },
                    onOpenProvider = { currentRoute = Route.Model.value },
                    onOpenRuntime = { currentRoute = Route.Runtime.value },
                    onProviderChange = { next ->
                        providerName = next.name
                        modelId = next.defaultModelId
                        baseUrl = next.defaultBaseUrl
                        extraConfig = next.extraFieldDefault
                        formErrors = FormErrors()
                        persistSettings(
                            currentState(
                                providerValue = next,
                                modelIdValue = next.defaultModelId,
                                baseUrlValue = next.defaultBaseUrl,
                                extraConfigValue = next.extraFieldDefault
                            )
                        )
                    },
                    onRuntimeChange = { next ->
                        runtimePreset = next
                        persistSettings(currentState(runtimePresetValue = next))
                    },
                )
            }

            Route.Model.value -> {
                ProviderSettingsScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { currentRoute = Route.Home.value },
                    onApiKeyChanged = {
                        apiKey = it
                        formErrors = formErrors.copy(apiKey = null)
                        persistSettings(currentState(apiKeyValue = it))
                    },
                    onModelIdChanged = {
                        modelId = it
                        formErrors = formErrors.copy(modelId = null)
                        persistSettings(currentState(modelIdValue = it))
                    },
                    onBaseUrlChanged = {
                        baseUrl = it
                        formErrors = formErrors.copy(baseUrl = null)
                        persistSettings(currentState(baseUrlValue = it))
                    },
                    onExtraConfigChanged = {
                        extraConfig = it
                        formErrors = formErrors.copy(extraConfig = null)
                        persistSettings(currentState(extraConfigValue = it))
                    },
                )
            }

            Route.Runtime.value -> {
                RuntimeSettingsScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { currentRoute = Route.Home.value },
                    onSystemPromptChanged = {
                        systemPrompt = it
                        persistSettings(currentState(systemPromptValue = it))
                    },
                    onTemperatureChanged = {
                        temperature = it
                        formErrors = formErrors.copy(temperature = null)
                        persistSettings(currentState(temperatureValue = it))
                    },
                    onMaxIterationsChanged = {
                        maxIterations = it
                        formErrors = formErrors.copy(maxIterations = null)
                        persistSettings(currentState(maxIterationsValue = it))
                    },
                )
            }
        }
    }
}

