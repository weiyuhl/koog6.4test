package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.serialization.serializeToolDescriptorsToJsonString
import com.example.myapplication.reflectbridge.ReflectBridgeSnapshotDto
import kotlinx.coroutines.launch

private object StudioColors {
    val page = Color(0xFFF7F8FB)
    val surface = Color(0xFFFFFFFF)
    val surfaceMuted = Color(0xFFF1F4F8)
    val border = Color(0xFFE3E8EF)
    val divider = Color(0xFFEDF1F5)
    val textPrimary = Color(0xFF111827)
    val textSecondary = Color(0xFF667085)
    val textHint = Color(0xFF98A2B3)
    val accent = Color(0xFF2563EB)
    val accentSoft = Color(0xFFEAF1FF)
    val userBubble = Color(0xFF2563EB)
    val userBubbleText = Color(0xFFFFFFFF)
    val assistantBubble = Color(0xFFFFFFFF)
    val systemBubble = Color(0xFFF4F6FA)
    val danger = Color(0xFFD92D20)
}

private enum class StudioMessageRole {
    User,
    Assistant,
    System,
}

private data class StudioChatMessage(
    val id: Long,
    val role: StudioMessageRole,
    val text: String,
    val label: String? = null,
)

private data class StudioFormErrors(
    val provider: String? = null,
    val modelId: String? = null,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val extraConfig: String? = null,
    val temperature: String? = null,
    val maxIterations: String? = null,
) {
    fun hasAny(): Boolean = listOf(provider, modelId, apiKey, baseUrl, extraConfig, temperature, maxIterations).any { it != null }
}

private data class StudioRuntimeSummary(
    val workspace: StudioWorkspace,
    val provider: KoogProvider,
    val modelId: String,
    val runtimePreset: AgentRuntimePreset,
    val localWriterEnabled: Boolean,
    val debuggerSettingEnabled: Boolean,
    val debuggerPort: String,
    val debuggerWaitMs: String,
    val remoteClientEnabled: Boolean,
    val remoteHost: String,
    val remotePort: String,
    val systemPromptLength: Int,
    val temperature: String,
    val maxIterations: String,
    val isRunning: Boolean,
    val messageCount: Int,
    val promptDraftLength: Int,
    val lastMessagePreview: String,
    val eventCount: Int,
    val toolCount: Int,
    val toolNames: List<String>,
    val lastRunId: String?,
    val lastStrategyName: String?,
    val lastNodeCount: Int,
    val lastSubgraphCount: Int,
    val lastToolCallCount: Int,
    val lastLlmCallCount: Int,
    val lastHistoryCount: Int,
    val lastStorageEntryCount: Int,
    val lastFeatureMessageCount: Int,
    val lastRemoteConnected: Boolean,
)

private const val STUDIO_STREAMING_PLACEHOLDER = "正在思考…"

private fun StoredChatMessage.toStudioMessage(): StudioChatMessage = StudioChatMessage(
    id = id,
    role = runCatching { StudioMessageRole.valueOf(role) }.getOrDefault(StudioMessageRole.System),
    text = text,
    label = label,
)

private fun StudioChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    role = role.name,
    text = text,
    label = label,
)

private fun String.preview(maxLength: Int = 120): String =
    replace('\n', ' ').trim().let { if (it.length <= maxLength) it else it.take(maxLength) + "…" }

@Composable
fun KoogStudioApp() {
    val appContext = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val localStore = remember(appContext) { AppLocalStore(appContext) }
    val restoredState = remember(localStore) { localStore.loadState() }
    val restoredWorkspace = remember(localStore) { StudioWorkspace.fromStoredRoute(localStore.loadLastWorkspaceRoute()) }
    val restoredRuntimePreset = remember(localStore) { AgentRuntimePreset.fromId(localStore.loadRuntimePresetId()) }
    val restoredProvider = remember(restoredState.settings.providerName) {
        KoogProvider.entries.firstOrNull { it.name == restoredState.settings.providerName } ?: KoogProvider.OPENAI
    }
    val restoredMessages = remember(restoredState.messages) { restoredState.messages.map { it.toStudioMessage() } }
    val demoTools = remember { demoToolsCatalog() }
    val toolSchemaJson = remember(demoTools) { serializeToolDescriptorsToJsonString(demoTools.map(Tool<*, *>::descriptor)) }

    var drawerOpen by remember { mutableStateOf(false) }
    var providerName by rememberSaveable { mutableStateOf(restoredProvider.name) }
    var apiKey by rememberSaveable { mutableStateOf(restoredState.settings.apiKey) }
    var modelId by rememberSaveable { mutableStateOf(restoredState.settings.modelId) }
    var baseUrl by rememberSaveable { mutableStateOf(restoredState.settings.baseUrl) }
    var extraConfig by rememberSaveable { mutableStateOf(restoredState.settings.extraConfig) }
    var prompt by rememberSaveable { mutableStateOf(restoredState.settings.promptDraft) }
    var runtimePreset by rememberSaveable { mutableStateOf(restoredRuntimePreset) }
    var localWriterEnabled by rememberSaveable { mutableStateOf(restoredState.settings.localWriterEnabled) }
    var debuggerEnabled by rememberSaveable { mutableStateOf(restoredState.settings.debuggerEnabled) }
    var debuggerPort by rememberSaveable { mutableStateOf(restoredState.settings.debuggerPort) }
    var debuggerWaitMs by rememberSaveable { mutableStateOf(restoredState.settings.debuggerWaitMs) }
    var remoteClientEnabled by rememberSaveable { mutableStateOf(restoredState.settings.remoteClientEnabled) }
    var remoteHost by rememberSaveable { mutableStateOf(restoredState.settings.remoteHost) }
    var remotePort by rememberSaveable { mutableStateOf(restoredState.settings.remotePort) }
    var reflectBridgeEnabled by rememberSaveable { mutableStateOf(restoredState.settings.reflectBridgeEnabled) }
    var reflectBridgeBaseUrl by rememberSaveable { mutableStateOf(restoredState.settings.reflectBridgeBaseUrl) }
    var systemPrompt by rememberSaveable { mutableStateOf(restoredState.settings.systemPrompt) }
    var temperature by rememberSaveable { mutableStateOf(restoredState.settings.temperature) }
    var maxIterations by rememberSaveable { mutableStateOf(restoredState.settings.maxIterations) }
    var formErrors by remember { mutableStateOf(StudioFormErrors()) }
    var isRunning by remember { mutableStateOf(false) }
    var lastRuntimeSnapshot by remember { mutableStateOf<AgentRuntimeSnapshot?>(null) }
    var nextMessageId by remember { mutableLongStateOf((restoredMessages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val messages = remember { mutableStateListOf<StudioChatMessage>().apply { addAll(restoredMessages) } }

    val provider = remember(providerName) { KoogProvider.valueOf(providerName) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentWorkspace = StudioWorkspace.fromStoredRoute(backStackEntry?.destination?.route ?: restoredWorkspace.route)
    val eventMessages = remember(messages.toList()) { messages.filter { it.label == "执行日志" || it.label == "错误" } }
    val runtimeSummary = remember(
        currentWorkspace,
        provider,
        modelId,
        runtimePreset,
        localWriterEnabled,
        debuggerEnabled,
        debuggerPort,
        debuggerWaitMs,
        remoteClientEnabled,
        remoteHost,
        remotePort,
        systemPrompt,
        temperature,
        maxIterations,
        isRunning,
        messages.size,
        prompt,
        eventMessages.size,
        demoTools,
        lastRuntimeSnapshot,
    ) {
        StudioRuntimeSummary(
            workspace = currentWorkspace,
            provider = provider,
            modelId = modelId,
            runtimePreset = runtimePreset,
            localWriterEnabled = localWriterEnabled,
            debuggerSettingEnabled = debuggerEnabled,
            debuggerPort = debuggerPort,
            debuggerWaitMs = debuggerWaitMs,
            remoteClientEnabled = remoteClientEnabled,
            remoteHost = remoteHost,
            remotePort = remotePort,
            systemPromptLength = systemPrompt.length,
            temperature = temperature,
            maxIterations = maxIterations,
            isRunning = isRunning,
            messageCount = messages.size,
            promptDraftLength = prompt.length,
            lastMessagePreview = messages.lastOrNull()?.text?.preview().orEmpty(),
            eventCount = lastRuntimeSnapshot?.timeline?.size ?: eventMessages.size,
            toolCount = demoTools.size,
            toolNames = demoTools.map { it.name },
            lastRunId = lastRuntimeSnapshot?.runId,
            lastStrategyName = lastRuntimeSnapshot?.strategyName,
            lastNodeCount = lastRuntimeSnapshot?.nodeNames?.size ?: 0,
            lastSubgraphCount = lastRuntimeSnapshot?.subgraphNames?.size ?: 0,
            lastToolCallCount = lastRuntimeSnapshot?.toolNames?.size ?: 0,
            lastLlmCallCount = lastRuntimeSnapshot?.llmModels?.size ?: 0,
            lastHistoryCount = lastRuntimeSnapshot?.historyCount ?: 0,
            lastStorageEntryCount = lastRuntimeSnapshot?.storageEntries?.size ?: 0,
            lastFeatureMessageCount = lastRuntimeSnapshot?.featureMessages?.size ?: 0,
            lastRemoteConnected = lastRuntimeSnapshot?.remoteClientConnected == true,
        )
    }

    LaunchedEffect(currentWorkspace) {
        localStore.saveLastWorkspaceRoute(currentWorkspace.route)
    }

    LaunchedEffect(runtimePreset) {
        localStore.saveRuntimePresetId(runtimePreset.id)
    }

    fun persistSettings(
        providerNameValue: String = providerName,
        apiKeyValue: String = apiKey,
        modelIdValue: String = modelId,
        baseUrlValue: String = baseUrl,
        extraConfigValue: String = extraConfig,
        promptValue: String = prompt,
        localWriterEnabledValue: Boolean = localWriterEnabled,
        debuggerEnabledValue: Boolean = debuggerEnabled,
        debuggerPortValue: String = debuggerPort,
        debuggerWaitMsValue: String = debuggerWaitMs,
        remoteClientEnabledValue: Boolean = remoteClientEnabled,
        remoteHostValue: String = remoteHost,
        remotePortValue: String = remotePort,
        reflectBridgeEnabledValue: Boolean = reflectBridgeEnabled,
        reflectBridgeBaseUrlValue: String = reflectBridgeBaseUrl,
        systemPromptValue: String = systemPrompt,
        temperatureValue: String = temperature,
        maxIterationsValue: String = maxIterations,
    ) {
        localStore.saveSettings(
            StoredSettings(
                providerName = providerNameValue,
                apiKey = apiKeyValue,
                modelId = modelIdValue,
                baseUrl = baseUrlValue,
                extraConfig = extraConfigValue,
                promptDraft = promptValue,
                localWriterEnabled = localWriterEnabledValue,
                debuggerEnabled = debuggerEnabledValue,
                debuggerPort = debuggerPortValue,
                debuggerWaitMs = debuggerWaitMsValue,
                remoteClientEnabled = remoteClientEnabledValue,
                remoteHost = remoteHostValue,
                remotePort = remotePortValue,
                reflectBridgeEnabled = reflectBridgeEnabledValue,
                reflectBridgeBaseUrl = reflectBridgeBaseUrlValue,
                systemPrompt = systemPromptValue,
                temperature = temperatureValue,
                maxIterations = maxIterationsValue,
            )
        )
    }

    fun persistMessages() {
        localStore.saveMessages(messages.map(StudioChatMessage::toStoredMessage))
    }

    fun addMessage(role: StudioMessageRole, text: String, label: String? = null): Long {
        val message = StudioChatMessage(id = nextMessageId++, role = role, text = text, label = label)
        messages += message
        persistMessages()
        return message.id
    }

    fun updateMessage(messageId: Long, update: (StudioChatMessage) -> StudioChatMessage) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages[index] = update(messages[index])
            persistMessages()
        }
    }

    fun removeMessage(messageId: Long) {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            messages.removeAt(index)
            persistMessages()
        }
    }

    fun applyProvider(next: KoogProvider) {
        providerName = next.name
        modelId = next.defaultModelId
        baseUrl = next.defaultBaseUrl
        extraConfig = next.extraFieldDefault
        formErrors = StudioFormErrors()
        persistSettings(
            providerNameValue = next.name,
            modelIdValue = next.defaultModelId,
            baseUrlValue = next.defaultBaseUrl,
            extraConfigValue = next.extraFieldDefault,
        )
    }

    fun validateSettings(): StudioFormErrors = StudioFormErrors(
        provider = if (!provider.isSupportedOnAndroid) "当前 Android 版本暂不支持该供应商，请切换到其他供应商。" else null,
        modelId = if (modelId.isBlank()) "请输入模型 ID" else null,
        apiKey = if (provider.requiresApiKey && apiKey.isBlank()) "请先输入 API Key" else null,
        baseUrl = if ((provider == KoogProvider.AZURE_OPENAI || provider == KoogProvider.OLLAMA) && baseUrl.isBlank()) "该供应商需要 Base URL" else null,
        extraConfig = if (provider.extraFieldLabel != null && extraConfig.isBlank()) "请填写该供应商所需的额外配置" else null,
        temperature = when {
            temperature.isBlank() -> "请输入 temperature"
            temperature.toDoubleOrNull() == null -> "temperature 必须是数字"
            temperature.toDouble() < 0.0 -> "temperature 不能小于 0"
            else -> null
        },
        maxIterations = when {
            maxIterations.isBlank() -> "请输入 max iterations"
            maxIterations.toIntOrNull() == null -> "max iterations 必须是整数"
            maxIterations.toInt() <= 0 -> "max iterations 必须大于 0"
            else -> null
        },
    )

    fun settingsSummary(errors: StudioFormErrors): String = buildList {
        errors.provider?.let(::add)
        errors.modelId?.let(::add)
        errors.apiKey?.let(::add)
        errors.baseUrl?.let(::add)
        errors.extraConfig?.let(::add)
        errors.temperature?.let(::add)
        errors.maxIterations?.let(::add)
    }.joinToString("；")

    fun buildRequest(userPrompt: String): AgentRequest = AgentRequest(
        provider = provider,
        apiKey = apiKey.trim(),
        modelId = modelId.trim(),
        baseUrl = baseUrl.trim(),
        extraConfig = extraConfig.trim(),
        runtimePreset = runtimePreset,
        systemPrompt = systemPrompt.trim(),
        temperature = temperature.trim().toDoubleOrNull(),
        maxIterations = maxIterations.trim().toIntOrNull(),
        featureConfig = AgentFeatureConfig(
            localWriterEnabled = localWriterEnabled,
            debuggerEnabled = debuggerEnabled,
            debuggerPort = debuggerPort.trim().toIntOrNull(),
            debuggerWaitMillis = debuggerWaitMs.trim().toLongOrNull(),
            remoteClientEnabled = remoteClientEnabled,
            remoteHost = remoteHost.trim(),
            remotePort = remotePort.trim().toIntOrNull(),
        ),
        userPrompt = userPrompt.trim(),
    )

    fun clearChat() {
        messages.clear()
        persistMessages()
        addMessage(role = StudioMessageRole.System, label = "新对话", text = "对话已清空。现在可以重新开始聊天。")
        drawerOpen = false
    }

    fun navigateTo(workspace: StudioWorkspace) {
        drawerOpen = false
        navController.navigate(workspace.route) {
            launchSingleTop = true
        }
    }

    fun submitPrompt() {
        val userPrompt = prompt.trim()
        if (userPrompt.isBlank() || isRunning) return

        val validation = validateSettings()
        formErrors = validation
        if (validation.hasAny()) {
            addMessage(
                role = StudioMessageRole.System,
                label = "设置未完成",
                text = "当前还不能发送消息，请先到 Agent Config 完善配置：${settingsSummary(validation)}",
            )
            navigateTo(StudioWorkspace.AgentConfig)
            return
        }

        addMessage(role = StudioMessageRole.User, text = userPrompt)
        val assistantMessageId = addMessage(
            role = StudioMessageRole.Assistant,
            label = provider.displayName,
            text = STUDIO_STREAMING_PLACEHOLDER,
        )
        prompt = ""
        persistSettings(promptValue = "")
        isRunning = true
        lastRuntimeSnapshot = null

        scope.launch {
            try {
                val result = KoogAgentRunner.runAgentStreaming(
                    request = buildRequest(userPrompt),
                    onTextDelta = { delta ->
                        updateMessage(assistantMessageId) { current ->
                            val nextText = if (current.text == STUDIO_STREAMING_PLACEHOLDER) delta else current.text + delta
                            current.copy(text = nextText)
                        }
                    }
                )
                lastRuntimeSnapshot = result.runtimeSnapshot
                updateMessage(assistantMessageId) { current ->
                    val mergedText = current.text.takeUnless { it.isBlank() || it == STUDIO_STREAMING_PLACEHOLDER }
                    current.copy(text = mergedText ?: result.answer)
                }
                if (result.events.isNotEmpty()) {
                    addMessage(role = StudioMessageRole.System, label = "执行日志", text = result.events.joinToString("\n"))
                }
            } catch (error: Throwable) {
                removeMessage(assistantMessageId)
                val message = error.message ?: error::class.simpleName ?: "Unknown error"
                addMessage(role = StudioMessageRole.System, label = "错误", text = message)
            } finally {
                isRunning = false
            }
        }
    }

    val drawerOffset by animateDpAsState(if (drawerOpen) 0.dp else (-320).dp, animationSpec = tween(220), label = "drawer")
    val scrimAlpha by animateFloatAsState(if (drawerOpen) 0.28f else 0f, animationSpec = tween(220), label = "scrim")

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(modifier = Modifier.fillMaxSize().background(StudioColors.page)) {
        NavHost(navController = navController, startDestination = restoredWorkspace.route) {
            composable(StudioWorkspace.Chat.route) {
                ChatWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    messages = messages,
                    prompt = prompt,
                    isRunning = isRunning,
                    onMenuClick = { drawerOpen = true },
                    onPromptChanged = {
                        prompt = it
                        persistSettings(promptValue = it)
                    },
                    onSendClick = ::submitPrompt,
                )
            }
            composable(StudioWorkspace.AgentConfig.route) {
                AgentConfigWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    provider = provider,
                    runtimePreset = runtimePreset,
                    apiKey = apiKey,
                    modelId = modelId,
                    baseUrl = baseUrl,
                    extraConfig = extraConfig,
                    localWriterEnabled = localWriterEnabled,
                    debuggerEnabled = debuggerEnabled,
                    debuggerPort = debuggerPort,
                    debuggerWaitMs = debuggerWaitMs,
                    remoteClientEnabled = remoteClientEnabled,
                    remoteHost = remoteHost,
                    remotePort = remotePort,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxIterations = maxIterations,
                    reflectBridgeEnabled = reflectBridgeEnabled,
                    reflectBridgeBaseUrl = reflectBridgeBaseUrl,
                    errors = formErrors,
                    onMenuClick = { drawerOpen = true },
                    onProviderSelected = ::applyProvider,
                    onRuntimePresetSelected = { runtimePreset = it },
                    onApiKeyChanged = {
                        apiKey = it
                        formErrors = formErrors.copy(apiKey = null)
                        persistSettings(apiKeyValue = it)
                    },
                    onModelIdChanged = {
                        modelId = it
                        formErrors = formErrors.copy(modelId = null)
                        persistSettings(modelIdValue = it)
                    },
                    onBaseUrlChanged = {
                        baseUrl = it
                        formErrors = formErrors.copy(baseUrl = null)
                        persistSettings(baseUrlValue = it)
                    },
                    onExtraConfigChanged = {
                        extraConfig = it
                        formErrors = formErrors.copy(extraConfig = null)
                        persistSettings(extraConfigValue = it)
                    },
                    onLocalWriterEnabledChanged = {
                        localWriterEnabled = it
                        persistSettings(localWriterEnabledValue = it)
                    },
                    onDebuggerEnabledChanged = {
                        debuggerEnabled = it
                        persistSettings(debuggerEnabledValue = it)
                    },
                    onDebuggerPortChanged = {
                        debuggerPort = it
                        persistSettings(debuggerPortValue = it)
                    },
                    onDebuggerWaitMsChanged = {
                        debuggerWaitMs = it
                        persistSettings(debuggerWaitMsValue = it)
                    },
                    onRemoteClientEnabledChanged = {
                        remoteClientEnabled = it
                        persistSettings(remoteClientEnabledValue = it)
                    },
                    onRemoteHostChanged = {
                        remoteHost = it
                        persistSettings(remoteHostValue = it)
                    },
                    onRemotePortChanged = {
                        remotePort = it
                        persistSettings(remotePortValue = it)
                    },
                    onSystemPromptChanged = {
                        systemPrompt = it
                        persistSettings(systemPromptValue = it)
                    },
                    onTemperatureChanged = {
                        temperature = it
                        persistSettings(temperatureValue = it)
                    },
                    onMaxIterationsChanged = {
                        maxIterations = it
                        persistSettings(maxIterationsValue = it)
                    },
                    onReflectBridgeEnabledChanged = {
                        reflectBridgeEnabled = it
                        persistSettings(reflectBridgeEnabledValue = it)
                    },
                    onReflectBridgeBaseUrlChanged = {
                        reflectBridgeBaseUrl = it
                        persistSettings(reflectBridgeBaseUrlValue = it)
                    },
                )
            }
            composable(StudioWorkspace.StrategyLab.route) {
                StrategyLabWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    onRuntimePresetSelected = { runtimePreset = it },
                    onMenuClick = { drawerOpen = true },
                )
            }
            composable(StudioWorkspace.ToolRegistry.route) {
                ToolRegistryWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    tools = demoTools,
                    toolSchemaJson = toolSchemaJson,
                    reflectBridgeEnabled = reflectBridgeEnabled,
                    reflectBridgeBaseUrl = reflectBridgeBaseUrl,
                    onMenuClick = { drawerOpen = true },
                )
            }
            composable(StudioWorkspace.EventsDebug.route) {
                EventsDebugWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    eventMessages = eventMessages,
                    runtimeSnapshot = lastRuntimeSnapshot,
                    onMenuClick = { drawerOpen = true },
                )
            }
            composable(StudioWorkspace.SessionInspector.route) {
                SessionInspectorWorkspaceScreen(
                    runtimeSummary = runtimeSummary,
                    messages = messages,
                    prompt = prompt,
                    runtimeSnapshot = lastRuntimeSnapshot,
                    onMenuClick = { drawerOpen = true },
                )
            }
        }

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)).clickable { drawerOpen = false },
            )
        }

        WorkspaceDrawer(
            currentWorkspace = currentWorkspace,
            provider = provider,
            offsetX = drawerOffset,
            onSelectWorkspace = ::navigateTo,
            onClearChat = ::clearChat,
            onCloseDrawer = { drawerOpen = false },
        )
    }
}

@Composable
private fun ChatWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    messages: List<StudioChatMessage>,
    prompt: String,
    isRunning: Boolean,
    onMenuClick: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isRunning) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().background(StudioColors.page).statusBarsPadding()) {
        StudioTopBar(workspace = runtimeSummary.workspace, supportingText = "当前供应商：${runtimeSummary.provider.displayName}", onMenuClick = onMenuClick)
        DividerLine()

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        DividerLine()
        ChatComposer(
            value = prompt,
            enabled = !isRunning,
            onValueChange = onPromptChanged,
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun AgentConfigWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    provider: KoogProvider,
    runtimePreset: AgentRuntimePreset,
    apiKey: String,
    modelId: String,
    baseUrl: String,
    extraConfig: String,
    localWriterEnabled: Boolean,
    debuggerEnabled: Boolean,
    debuggerPort: String,
    debuggerWaitMs: String,
    remoteClientEnabled: Boolean,
    remoteHost: String,
    remotePort: String,
    systemPrompt: String,
    temperature: String,
    maxIterations: String,
    reflectBridgeEnabled: Boolean,
    reflectBridgeBaseUrl: String,
    errors: StudioFormErrors,
    onMenuClick: () -> Unit,
    onProviderSelected: (KoogProvider) -> Unit,
    onRuntimePresetSelected: (AgentRuntimePreset) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
    onLocalWriterEnabledChanged: (Boolean) -> Unit,
    onDebuggerEnabledChanged: (Boolean) -> Unit,
    onDebuggerPortChanged: (String) -> Unit,
    onDebuggerWaitMsChanged: (String) -> Unit,
    onRemoteClientEnabledChanged: (Boolean) -> Unit,
    onRemoteHostChanged: (String) -> Unit,
    onRemotePortChanged: (String) -> Unit,
    onSystemPromptChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxIterationsChanged: (String) -> Unit,
    onReflectBridgeEnabledChanged: (Boolean) -> Unit,
    onReflectBridgeBaseUrlChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(StudioColors.page).statusBarsPadding()) {
        StudioTopBar(workspace = runtimeSummary.workspace, supportingText = runtimeSummary.workspace.subtitle, onMenuClick = onMenuClick)
        DividerLine()

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StudioSection(title = "当前运行摘要", subtitle = "Phase 1 已经把配置页纳入统一工作区壳，后续 Phase 2 会继续接入更多 agents-core 运行模式。") {
                InfoRow(label = "当前供应商", value = runtimeSummary.provider.displayName)
                InfoRow(label = "当前模型", value = runtimeSummary.modelId.ifBlank { "未设置" })
                InfoRow(label = "当前预设", value = runtimeSummary.runtimePreset.title)
                InfoRow(label = "Temperature / MaxIter", value = "${runtimeSummary.temperature} / ${runtimeSummary.maxIterations}")
                InfoRow(label = "System prompt", value = if (runtimeSummary.systemPromptLength > 0) "${runtimeSummary.systemPromptLength} chars" else "未设置")
                InfoRow(label = "当前状态", value = if (runtimeSummary.isRunning) "运行中" else "空闲")
            }

            StudioSection(title = "Agent 运行预设", subtitle = "这里开始接入 agents-core 主干模式。Chat 工作区发送消息时会按当前预设运行。") {
                AgentRuntimePreset.entries.forEach { preset ->
                    PresetRow(
                        title = preset.title,
                        note = "${preset.family} · ${preset.description}",
                        selected = preset == runtimePreset,
                        onClick = { onRuntimePresetSelected(preset) },
                    )
                }
            }

            StudioSection(title = "供应商", subtitle = "选择当前聊天和运行入口使用的 LLM 提供方") {
                KoogProvider.entries.forEach { item ->
                    ProviderRow(
                        label = item.displayName,
                        note = item.providerNote,
                        selected = item == provider,
                        enabled = item.isSupportedOnAndroid,
                        onClick = { onProviderSelected(item) },
                    )
                }
                errors.provider?.let { InlineError(text = it) }
            }

            StudioSection(title = "模型与连接", subtitle = "这些配置会直接影响 Chat 工作区和后续 Strategy/Tools 工作区的运行上下文。") {
                SettingsTextField(
                    label = "模型 ID",
                    value = modelId,
                    onValueChange = onModelIdChanged,
                    placeholder = provider.defaultModelId,
                    error = errors.modelId,
                )

                if (provider.requiresApiKey) {
                    SettingsTextField(
                        label = "API Key",
                        value = apiKey,
                        onValueChange = onApiKeyChanged,
                        placeholder = "输入供应商对应的密钥",
                        error = errors.apiKey,
                        obscureText = true,
                    )
                }

                if (provider != KoogProvider.BEDROCK) {
                    SettingsTextField(
                        label = provider.baseUrlLabel,
                        value = baseUrl,
                        onValueChange = onBaseUrlChanged,
                        placeholder = provider.defaultBaseUrl.ifBlank { "https://example.com" },
                        error = errors.baseUrl,
                        keyboardType = KeyboardType.Uri,
                    )
                }

                provider.extraFieldLabel?.let { extraLabel ->
                    SettingsTextField(
                        label = extraLabel,
                        value = extraConfig,
                        onValueChange = onExtraConfigChanged,
                        placeholder = provider.extraFieldDefault,
                        error = errors.extraConfig,
                    )
                }
            }

            StudioSection(title = "Prompt 与运行参数", subtitle = "补齐 system prompt / temperature / max iterations 等通用 agent 配置入口。") {
                SettingsTextField(
                    label = "System prompt",
                    value = systemPrompt,
                    onValueChange = onSystemPromptChanged,
                    placeholder = "可选：附加给 Koog 运行器的系统提示",
                    error = null,
                    singleLine = false,
                )
                SettingsTextField(
                    label = "Temperature",
                    value = temperature,
                    onValueChange = onTemperatureChanged,
                    placeholder = "0.2",
                    error = errors.temperature,
                    keyboardType = KeyboardType.Decimal,
                )
                SettingsTextField(
                    label = "Max iterations",
                    value = maxIterations,
                    onValueChange = onMaxIterationsChanged,
                    placeholder = "50",
                    error = errors.maxIterations,
                    keyboardType = KeyboardType.Number,
                )
            }

            StudioSection(title = "Core feature bridge", subtitle = "这里接入 writer / debugger / remote client 的配置入口，供 Events 工作区与 Runner 共享。") {
                ToggleRow(
                    title = "本地 writer sink",
                    note = "把 feature messages 写入本地 UI 面板，便于直接观察调试输出。",
                    checked = localWriterEnabled,
                    onToggle = onLocalWriterEnabledChanged,
                )
                ToggleRow(
                    title = "启用 debugger server",
                    note = "在 agent 上安装 Debugger feature，暴露 remote server 端口。",
                    checked = debuggerEnabled,
                    onToggle = onDebuggerEnabledChanged,
                )
                SettingsTextField(
                    label = "Debugger port",
                    value = debuggerPort,
                    onValueChange = onDebuggerPortChanged,
                    placeholder = "50881",
                    error = null,
                    keyboardType = KeyboardType.Number,
                )
                SettingsTextField(
                    label = "Debugger wait (ms)",
                    value = debuggerWaitMs,
                    onValueChange = onDebuggerWaitMsChanged,
                    placeholder = "250",
                    error = null,
                    keyboardType = KeyboardType.Number,
                )
                ToggleRow(
                    title = "启用 remote client",
                    note = "运行时尝试通过 SSE 连接指定 remote feature server，并把收到的消息回灌到 Events 页面。",
                    checked = remoteClientEnabled,
                    onToggle = onRemoteClientEnabledChanged,
                )
                SettingsTextField(
                    label = "Remote host",
                    value = remoteHost,
                    onValueChange = onRemoteHostChanged,
                    placeholder = "127.0.0.1",
                    error = null,
                )
                SettingsTextField(
                    label = "Remote port",
                    value = remotePort,
                    onValueChange = onRemotePortChanged,
                    placeholder = "50881",
                    error = null,
                    keyboardType = KeyboardType.Number,
                )
            }

            StudioSection(title = "JVM reflect bridge", subtitle = "用于接入 JVM 专属 reflect tools。先在本机启动 host，再让 Android 端通过 HTTP 读取和调用。") {
                ToggleRow(
                    title = "启用 reflect bridge",
                    note = "开启后，Tool Registry 会尝试从外部 JVM host 加载 ToolFromCallable / ToolSet / asTool / asTools 能力。",
                    checked = reflectBridgeEnabled,
                    onToggle = onReflectBridgeEnabledChanged,
                )
                SettingsTextField(
                    label = "Bridge base URL",
                    value = reflectBridgeBaseUrl,
                    onValueChange = onReflectBridgeBaseUrlChanged,
                    placeholder = "http://10.0.2.2:8095",
                    error = null,
                )
                InfoRow(label = "Host 启动命令", value = ".\\gradlew.bat :reflect-bridge-host:run")
            }
        }
    }
}

@Composable
private fun StrategyLabWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    onRuntimePresetSelected: (AgentRuntimePreset) -> Unit,
    onMenuClick: () -> Unit,
) {
    val presets = listOf(
        "Session/context storage bridge（继续增强）",
        "Subgraph retry / task helpers（后续阶段）",
        "Debugger / remote bridge（已接通，继续增强）",
    )

    WorkspaceScrollPage(workspace = runtimeSummary.workspace, onMenuClick = onMenuClick) {
        StudioSection(title = "已接通的运行预设", subtitle = "这些模式已经能驱动当前 Chat 工作区实际运行。") {
            AgentRuntimePreset.entries.forEach { preset ->
                PresetRow(
                    title = preset.title,
                    note = "${preset.family} · streaming=${preset.supportsStreaming} · tools=${preset.usesTools}",
                    selected = preset == runtimeSummary.runtimePreset,
                    onClick = { onRuntimePresetSelected(preset) },
                )
            }
        }

        StudioSection(title = "当前运行模式", subtitle = "当前聊天页会按这里选中的预设执行。") {
            InfoRow(label = "当前预设", value = runtimeSummary.runtimePreset.title)
            InfoRow(label = "Agent 家族", value = runtimeSummary.runtimePreset.family)
            InfoRow(label = "当前 provider", value = runtimeSummary.provider.displayName)
            InfoRow(label = "当前 modelId", value = runtimeSummary.modelId.ifBlank { "未设置" })
            InfoRow(label = "工具数量", value = runtimeSummary.toolCount.toString())
            InfoRow(label = "最近 runId", value = runtimeSummary.lastRunId ?: "暂无")
            InfoRow(label = "最近 strategy", value = runtimeSummary.lastStrategyName ?: "暂无")
            InfoRow(label = "history / storage", value = "${runtimeSummary.lastHistoryCount} / ${runtimeSummary.lastStorageEntryCount}")
            InfoRow(label = "feature messages", value = runtimeSummary.lastFeatureMessageCount.toString())
            InfoRow(label = "消息数量", value = runtimeSummary.messageCount.toString())
        }

        StudioSection(title = "下一步预设", subtitle = "这些能力会在后续继续补齐到 Strategy Lab。") {
            presets.forEachIndexed { index, item ->
                InfoRow(label = "Todo ${index + 1}", value = item)
            }
        }
    }
}

@Composable
private fun ToolRegistryWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    tools: List<Tool<*, *>>,
    toolSchemaJson: String,
    reflectBridgeEnabled: Boolean,
    reflectBridgeBaseUrl: String,
    onMenuClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bridgeClient = remember { ReflectBridgeClient() }
    var bridgeSnapshot by remember { mutableStateOf<ReflectBridgeSnapshotDto?>(null) }
    var bridgeError by remember { mutableStateOf<String?>(null) }
    var isRefreshingBridge by remember { mutableStateOf(false) }
    val toolDefinitions = remember(tools, bridgeSnapshot, reflectBridgeBaseUrl) {
        tools.map { it.asWorkbenchDefinition() } + bridgeSnapshot?.tools.orEmpty().map { it.asWorkbenchDefinition(bridgeClient, reflectBridgeBaseUrl) }
    }
    var selectedToolName by remember(toolDefinitions) { mutableStateOf(toolDefinitions.firstOrNull()?.name.orEmpty()) }
    val selectedTool = remember(selectedToolName, toolDefinitions) { toolDefinitions.firstOrNull { it.name == selectedToolName } }
    var rawInputs by remember(selectedTool?.name) {
        mutableStateOf(selectedTool?.parameters?.associate { it.name to "" }.orEmpty())
    }
    var latestRecord by remember { mutableStateOf<ToolWorkbenchExecutionRecord?>(null) }
    val executionHistory = remember { mutableStateListOf<ToolWorkbenchExecutionRecord>() }
    var executionError by remember { mutableStateOf<String?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    fun refreshBridge() {
        if (!reflectBridgeEnabled) {
            bridgeSnapshot = null
            bridgeError = null
            return
        }
        scope.launch {
            isRefreshingBridge = true
            bridgeError = null
            try {
                bridgeSnapshot = bridgeClient.fetchSnapshot(reflectBridgeBaseUrl)
            } catch (error: Throwable) {
                bridgeSnapshot = null
                bridgeError = error.message ?: error::class.simpleName ?: "Unknown error"
            } finally {
                isRefreshingBridge = false
            }
        }
    }

    LaunchedEffect(reflectBridgeEnabled, reflectBridgeBaseUrl) {
        refreshBridge()
    }

    LaunchedEffect(toolDefinitions.map { it.name }) {
        if (toolDefinitions.none { it.name == selectedToolName }) {
            selectedToolName = toolDefinitions.firstOrNull()?.name.orEmpty()
        }
    }

    val reflectToolCount = bridgeSnapshot?.tools?.size ?: 0
    val bridgeDiagnostics = remember(bridgeSnapshot) { bridgeClient.snapshotDiagnosticsToWorkbench(bridgeSnapshot?.diagnostics.orEmpty()) }
    val totalToolCount = toolDefinitions.size

    WorkspaceScrollPage(workspace = runtimeSummary.workspace, onMenuClick = onMenuClick) {
        StudioSection(title = "当前 registry 摘要", subtitle = "这里现在同时聚合 common tools 与 JVM reflect bridge tools。") {
            InfoRow(label = "总工具数", value = totalToolCount.toString())
            InfoRow(label = "Common / Reflect", value = "${tools.size} / $reflectToolCount")
            InfoRow(label = "Reflect bridge", value = if (reflectBridgeEnabled) "已启用" else "关闭")
            InfoRow(label = "Bridge URL", value = reflectBridgeBaseUrl.ifBlank { "未设置" })
            InfoRow(label = "当前运行预设", value = runtimeSummary.runtimePreset.title)
        }

        StudioSection(title = "Reflect bridge 状态", subtitle = "先在本机运行 host，再点击刷新加载 JVM reflect tools。") {
            InfoRow(label = "Host 命令", value = ".\\gradlew.bat :reflect-bridge-host:run")
            InfoRow(label = "Bridge 开关", value = if (reflectBridgeEnabled) "已启用" else "关闭")
            InfoRow(label = "Bridge diagnostics", value = bridgeDiagnostics.size.toString())
            InfoRow(label = "最近加载结果", value = when {
                !reflectBridgeEnabled -> "未启用"
                isRefreshingBridge -> "加载中…"
                bridgeSnapshot != null -> "已连接到 ${bridgeSnapshot?.hostName}"
                else -> "尚未连接"
            })
            bridgeError?.let { InlineError(text = it) }
            ActionButton(
                text = if (isRefreshingBridge) "刷新中…" else "刷新 reflect tools",
                enabled = !isRefreshingBridge && reflectBridgeEnabled,
                onClick = ::refreshBridge,
            )
        }

        if (bridgeDiagnostics.isNotEmpty()) {
            StudioSection(title = "Reflect bridge diagnostics", subtitle = "这里展示 JVM host 在 reflect 注册阶段捕获到的失败项，例如非可序列化参数。") {
                bridgeDiagnostics.forEach { diagnostic ->
                    InfoRow(
                        label = "${diagnostic.failureKind} · ${diagnostic.registration}",
                        value = diagnostic.message,
                    )
                }
            }
        }

        StudioSection(title = "选择工具", subtitle = "这里统一展示 common tools 与 reflect bridge tools；来源和注册方式会显示在条目说明里。") {
            toolDefinitions.forEach { tool ->
                PresetRow(
                    title = tool.name,
                    note = "${tool.sourceLabel} · ${tool.registrationLabel} · ${tool.description}",
                    selected = tool.name == selectedToolName,
                    onClick = { selectedToolName = tool.name },
                )
            }
        }

        selectedTool?.let { tool ->
            val parameters = tool.parameters

            StudioSection(title = "Descriptor 细节", subtitle = "这里展示当前工具的真实参数定义，可直接对照表单调试。") {
                InfoRow(label = "工具名", value = tool.name)
                InfoRow(label = "描述", value = tool.description)
                InfoRow(label = "来源", value = tool.sourceLabel)
                InfoRow(label = "注册方式", value = tool.registrationLabel)
                InfoRow(label = "参数统计", value = "required=${parameters.count { it.isRequired }}, optional=${parameters.count { !it.isRequired }}")
                if (parameters.isEmpty()) {
                    InfoRow(label = "参数", value = "该工具无需输入参数")
                } else {
                    parameters.forEach { parameter ->
                        InfoRow(
                            label = "${parameter.name}${if (parameter.isRequired) " *" else ""}",
                            value = "${parameter.description} · ${parameter.typeLabel}",
                        )
                    }
                }
            }

            StudioSection(title = "执行表单", subtitle = "common tools 会在本地执行；reflect tools 会通过 JVM bridge host 远程执行。") {
                if (parameters.isEmpty()) {
                    InfoRow(label = "提示", value = "该工具没有输入参数，可直接执行。")
                } else {
                    parameters.forEach { parameter ->
                        val currentValue = rawInputs[parameter.name].orEmpty()
                        SettingsTextField(
                            label = "${parameter.name}${if (parameter.isRequired) " *" else ""}",
                            value = currentValue,
                            onValueChange = { next -> rawInputs = rawInputs.toMutableMap().apply { put(parameter.name, next) } },
                            placeholder = parameter.kind.toWorkbenchInputHint(parameter.enumValues),
                            error = null,
                            keyboardType = parameter.kind.toKeyboardType(),
                            singleLine = parameter.kind.isSingleLineInput(),
                        )
                    }
                }

                executionError?.let { InlineError(text = it) }
                ActionButton(
                    text = if (isExecuting) "执行中…" else "执行 ${tool.name}",
                    enabled = !isExecuting,
                    onClick = {
                        executionError = null
                        scope.launch {
                            isExecuting = true
                            try {
                                val record = tool.execute(rawInputs)
                                latestRecord = record
                                executionHistory.add(0, record)
                                while (executionHistory.size > 8) executionHistory.removeLast()
                            } catch (error: Throwable) {
                                executionError = error.message ?: error::class.simpleName ?: "Unknown error"
                            } finally {
                                isExecuting = false
                            }
                        }
                    },
                )
            }

            StudioSection(title = "最近执行结果", subtitle = "显示本地 workbench 最近一次执行的输入 JSON 与结果字符串。") {
                val record = latestRecord
                if (record == null) {
                    InfoRow(label = "状态", value = "还没有执行记录")
                } else {
                    InfoRow(label = "工具", value = record.toolName)
                    InfoRow(label = "来源", value = record.sourceLabel)
                    InfoRow(label = "注册方式", value = record.registrationLabel)
                    InfoRow(label = "时间", value = record.timestamp)
                    InfoRow(label = "状态", value = record.status)
                    record.failureKind?.let { InfoRow(label = "失败分类", value = it.name) }
                    InfoRow(label = "Args JSON", value = record.argsJson)
                    record.errorText?.let { InlineError(text = it) }
                    InfoRow(label = "Result", value = record.resultText.ifBlank { "-" })
                }
            }

            StudioSection(title = "执行历史", subtitle = "保留最近 8 次本地执行，方便对比参数和输出。") {
                if (executionHistory.isEmpty()) {
                    InfoRow(label = "状态", value = "暂无历史")
                } else {
                    executionHistory.forEachIndexed { index, record ->
                        val value = buildString {
                            append(record.sourceLabel)
                            append(" · ")
                            append(record.timestamp)
                            record.failureKind?.let {
                                append(" · ")
                                append(it.name)
                            }
                            if (record.resultText.isNotBlank()) {
                                append(" · ")
                                append(record.resultText)
                            } else if (!record.errorText.isNullOrBlank()) {
                                append(" · ")
                                append(record.errorText)
                            }
                        }
                        InfoRow(label = "#${index + 1} ${record.toolName}", value = value)
                    }
                }
            }

            StudioSection(title = "当前工具 Schema", subtitle = "这里展示当前所选工具的 schema/descriptor JSON；对于 reflect tool，这里来自 JVM bridge host。") {
                SelectionContainer {
                    BasicText(text = tool.schemaJson, style = bodyStyle(fontSize = 13.sp, lineHeight = 20.sp))
                }
            }
        }

        StudioSection(title = "Common registry schema JSON", subtitle = "这里保留 Android 端本地 common tool registry 的完整 schema JSON。") {
            SelectionContainer {
                BasicText(text = toolSchemaJson, style = bodyStyle(fontSize = 13.sp, lineHeight = 20.sp))
            }
        }
    }
}

@Composable
private fun EventsDebugWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    eventMessages: List<StudioChatMessage>,
    runtimeSnapshot: AgentRuntimeSnapshot?,
    onMenuClick: () -> Unit,
) {
    WorkspaceScrollPage(workspace = runtimeSummary.workspace, onMenuClick = onMenuClick) {
        StudioSection(title = "事件时间线摘要", subtitle = "这里现在同时显示 handler timeline、writer/debug 输出，以及 remote client/server 的最近状态。") {
            InfoRow(label = "事件条数", value = runtimeSummary.eventCount.toString())
            InfoRow(label = "运行状态", value = if (runtimeSummary.isRunning) "运行中" else "空闲")
            InfoRow(label = "最近 runId", value = runtimeSummary.lastRunId ?: "暂无")
            InfoRow(label = "node / subgraph", value = "${runtimeSummary.lastNodeCount} / ${runtimeSummary.lastSubgraphCount}")
            InfoRow(label = "llm / tool", value = "${runtimeSummary.lastLlmCallCount} / ${runtimeSummary.lastToolCallCount}")
            InfoRow(label = "history / storage", value = "${runtimeSummary.lastHistoryCount} / ${runtimeSummary.lastStorageEntryCount}")
            InfoRow(label = "feature messages", value = runtimeSummary.lastFeatureMessageCount.toString())
        }

        StudioSection(title = "Core feature config", subtitle = "这里反映当前配置页中启用的 writer / debugger / remote 入口。") {
            InfoRow(label = "Local writer", value = if (runtimeSummary.localWriterEnabled) "已启用" else "关闭")
            InfoRow(label = "Debugger server", value = if (runtimeSummary.debuggerSettingEnabled) "已启用" else "关闭")
            InfoRow(label = "Debugger port / wait", value = "${runtimeSummary.debuggerPort} / ${runtimeSummary.debuggerWaitMs} ms")
            InfoRow(label = "Remote client", value = if (runtimeSummary.remoteClientEnabled) "已启用" else "关闭")
            InfoRow(label = "Remote target", value = "${runtimeSummary.remoteHost}:${runtimeSummary.remotePort}")
        }

        StudioSection(title = "最近运行状态", subtitle = "这里展示最近一次真实 run 回传的 feature bridge 状态，而不是静态配置。") {
            InfoRow(label = "Debugger active", value = if (runtimeSnapshot?.debuggerEnabled == true) "是" else "否")
            InfoRow(label = "Local writer active", value = if (runtimeSnapshot?.localWriterEnabled == true) "是" else "否")
            InfoRow(label = "Debugger port", value = runtimeSnapshot?.debuggerPort?.toString() ?: "暂无")
            InfoRow(label = "Remote client active", value = if (runtimeSnapshot?.remoteClientEnabled == true) "是" else "否")
            InfoRow(label = "Remote target", value = runtimeSnapshot?.remoteClientTarget ?: "暂无")
            InfoRow(label = "Remote connected", value = if (runtimeSummary.lastRemoteConnected) "是" else "否")
        }

        if (runtimeSnapshot == null && eventMessages.isEmpty()) {
            StudioSection(title = "最近事件", subtitle = "还没有事件消息。发送一次聊天消息后，这里会出现 timeline 与执行日志。") {}
        } else if (runtimeSnapshot != null) {
            StudioSection(title = "真实 timeline", subtitle = "来自 EventHandler 捕获的 strategy / node / subgraph / llm / tool 生命周期。") {
                runtimeSnapshot.timeline.takeLast(12).forEach { entry ->
                    val value = listOfNotNull(entry.executionPath, entry.detail.takeIf { it.isNotBlank() }).joinToString(" · ")
                    InfoRow(label = "${entry.category} · ${entry.name}", value = value.ifBlank { "-" })
                }
            }
        } else {
            StudioSection(title = "最近事件", subtitle = "回退到聊天系统消息。") {
                eventMessages.takeLast(6).forEach { message ->
                    InfoRow(label = message.label ?: message.role.name, value = message.text.preview(160))
                }
            }
        }

        StudioSection(title = "Feature messages", subtitle = "这里展示 local writer / remote client 最近收集到的 feature messages。") {
            val featureMessages = runtimeSnapshot?.featureMessages.orEmpty()
            if (featureMessages.isEmpty()) {
                InfoRow(label = "状态", value = "暂无 feature message")
            } else {
                featureMessages.takeLast(12).forEach { entry ->
                    InfoRow(label = "${entry.source} · ${entry.type}", value = entry.detail)
                }
            }
        }

        StudioSection(title = "已接入的 core features", subtitle = "当前页面已经接入 timeline、writer/debug 输出、remote client 状态与最近消息流。") {
            InfoRow(label = "Handlers", value = "agent / llm / node / tool / strategy / streaming / subgraph")
            InfoRow(label = "Debugger", value = "server 配置、端口、运行状态")
            InfoRow(label = "Remote", value = "client 配置、连接状态、消息回灌")
        }
    }
}

@Composable
private fun SessionInspectorWorkspaceScreen(
    runtimeSummary: StudioRuntimeSummary,
    messages: List<StudioChatMessage>,
    prompt: String,
    runtimeSnapshot: AgentRuntimeSnapshot?,
    onMenuClick: () -> Unit,
) {
    WorkspaceScrollPage(workspace = runtimeSummary.workspace, onMenuClick = onMenuClick) {
        StudioSection(title = "会话摘要", subtitle = "这里现在会显示最近一次真实 run 的会话快照，而不只是 UI 状态。") {
            InfoRow(label = "当前工作区", value = runtimeSummary.workspace.title)
            InfoRow(label = "Provider", value = runtimeSummary.provider.displayName)
            InfoRow(label = "Model ID", value = runtimeSummary.modelId.ifBlank { "未设置" })
            InfoRow(label = "Runtime preset", value = runtimeSummary.runtimePreset.title)
            InfoRow(label = "Run ID", value = runtimeSnapshot?.runId ?: "暂无")
            InfoRow(label = "Agent ID", value = runtimeSnapshot?.agentId ?: "暂无")
            InfoRow(label = "Strategy", value = runtimeSnapshot?.strategyName ?: "暂无")
            InfoRow(label = "History", value = runtimeSnapshot?.historyCount?.toString() ?: "0")
            InfoRow(label = "Storage", value = runtimeSnapshot?.storageEntries?.size?.toString() ?: "0")
            InfoRow(label = "草稿长度", value = prompt.length.toString())
            InfoRow(label = "消息数量", value = messages.size.toString())
        }

        StudioSection(title = "执行结构", subtitle = "基于最近一次运行的 node / subgraph / llm / tool 统计。") {
            InfoRow(label = "Nodes", value = runtimeSnapshot?.nodeNames?.joinToString().orEmpty().ifBlank { "暂无" })
            InfoRow(label = "Subgraphs", value = runtimeSnapshot?.subgraphNames?.joinToString().orEmpty().ifBlank { "暂无" })
            InfoRow(label = "LLM models", value = runtimeSnapshot?.llmModels?.joinToString().orEmpty().ifBlank { "暂无" })
            InfoRow(label = "Tools", value = runtimeSnapshot?.toolNames?.joinToString().orEmpty().ifBlank { "暂无" })
        }

        StudioSection(title = "Context storage", subtitle = "这里展示 Runner 从 AIAgentContext.storage 抽取的真实键值摘要。") {
            val entries = runtimeSnapshot?.storageEntries.orEmpty()
            if (entries.isEmpty()) {
                InfoRow(label = "状态", value = "暂无 storage 数据")
            } else {
                entries.forEach { entry ->
                    InfoRow(label = entry.key, value = entry.valuePreview)
                }
            }
        }

        StudioSection(title = "Session history", subtitle = "这里展示最近一次运行后，从 agent context 抽取的消息历史预览。") {
            val historyEntries = runtimeSnapshot?.historyPreview.orEmpty()
            if (historyEntries.isEmpty()) {
                InfoRow(label = "状态", value = "暂无历史快照")
            } else {
                historyEntries.forEachIndexed { index, entry ->
                    InfoRow(label = "${index + 1}. ${entry.role}", value = entry.contentPreview)
                }
            }
        }

        StudioSection(title = "最后一条消息", subtitle = "为后续 run/session/context 面板预留观察位。") {
            val lastMessage = messages.lastOrNull()
            InfoRow(label = "角色", value = lastMessage?.role?.name ?: "无")
            InfoRow(label = "标签", value = lastMessage?.label ?: "无")
            InfoRow(label = "预览", value = lastMessage?.text?.preview(160) ?: "暂无")
        }

        StudioSection(title = "最近运行结果", subtitle = "使用 Runner 回传的结构化快照摘要。") {
            InfoRow(label = "最终结果预览", value = runtimeSnapshot?.finalResultPreview ?: "暂无")
            InfoRow(label = "Timeline 条数", value = runtimeSnapshot?.timeline?.size?.toString() ?: "0")
        }
    }
}

@Composable
private fun WorkspaceScrollPage(
    workspace: StudioWorkspace,
    onMenuClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(StudioColors.page).statusBarsPadding()) {
        StudioTopBar(workspace = workspace, supportingText = workspace.subtitle, onMenuClick = onMenuClick)
        DividerLine()
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
    }
}

@Composable
private fun StudioTopBar(workspace: StudioWorkspace, supportingText: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(StudioColors.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(text = "☰", onClick = onMenuClick)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = workspace.title, style = titleStyle())
            BasicText(text = supportingText, style = secondaryStyle())
        }
    }
}

@Composable
private fun WorkspaceDrawer(
    currentWorkspace: StudioWorkspace,
    provider: KoogProvider,
    offsetX: androidx.compose.ui.unit.Dp,
    onSelectWorkspace: (StudioWorkspace) -> Unit,
    onClearChat: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    Column(
        modifier = Modifier.statusBarsPadding().offset(x = offsetX).fillMaxHeight().width(320.dp).background(StudioColors.surface).border(1.dp, StudioColors.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = "Koog Studio", style = titleStyle())
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(text = "当前供应商：${provider.displayName}", style = secondaryStyle())
            }
            CircleIconButton(text = "×", onClick = onCloseDrawer)
        }

        DividerLine()
        DrawerActionItem(title = "清空当前对话", subtitle = "仅清理 Chat 工作区消息", selected = false, onClick = onClearChat)
        DividerLine()
        StudioWorkspace.entries.forEach { workspace ->
            DrawerActionItem(
                title = workspace.title,
                subtitle = workspace.subtitle,
                selected = workspace == currentWorkspace,
                onClick = { onSelectWorkspace(workspace) },
            )
        }
    }
}

@Composable
private fun DrawerActionItem(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(if (selected) StudioColors.accentSoft else StudioColors.surface).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(text = title, style = bodyStyle(fontWeight = FontWeight.SemiBold, color = if (selected) StudioColors.accent else StudioColors.textPrimary))
        BasicText(text = subtitle, style = secondaryStyle())
    }
}

@Composable
private fun MessageBubble(message: StudioChatMessage) {
    val isUser = message.role == StudioMessageRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when (message.role) {
        StudioMessageRole.User -> StudioColors.userBubble
        StudioMessageRole.Assistant -> StudioColors.assistantBubble
        StudioMessageRole.System -> StudioColors.systemBubble
    }
    val textColor = if (isUser) StudioColors.userBubbleText else StudioColors.textPrimary

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f).clip(RoundedCornerShape(20.dp)).background(bubbleColor).border(
                1.dp,
                if (isUser) StudioColors.userBubble else StudioColors.border,
                RoundedCornerShape(20.dp),
            ).padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            message.label?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = if (isUser) StudioColors.userBubbleText.copy(alpha = 0.88f) else StudioColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            SelectionContainer {
                BasicText(text = message.text, style = TextStyle(color = textColor, fontSize = 15.sp, lineHeight = 22.sp))
            }
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(StudioColors.surface).navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(color = StudioColors.textPrimary, fontSize = 15.sp, lineHeight = 21.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            cursorBrush = SolidColor(StudioColors.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { innerField ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StudioColors.surfaceMuted).border(
                        1.dp,
                        StudioColors.border,
                        RoundedCornerShape(24.dp),
                    ).padding(horizontal = 16.dp, vertical = 14.dp).heightIn(min = 52.dp),
                ) {
                    if (value.isBlank()) {
                        BasicText(text = "输入消息，其他工作区在左侧抽屉中", style = TextStyle(color = StudioColors.textHint, fontSize = 15.sp))
                    }
                    innerField()
                }
            },
        )

        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(if (enabled) StudioColors.accent else StudioColors.textHint).clickable(enabled = enabled, onClick = onSendClick),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "发", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun StudioSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(StudioColors.surface).border(
            1.dp,
            StudioColors.border,
            RoundedCornerShape(20.dp),
        ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText(text = title, style = titleStyle())
        BasicText(text = subtitle, style = secondaryStyle())
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(text = label, style = bodyStyle(fontSize = 13.sp, color = StudioColors.textSecondary, fontWeight = FontWeight.Medium))
        BasicText(text = value, style = bodyStyle())
    }
}

@Composable
private fun ProviderRow(
    label: String,
    note: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) StudioColors.accentSoft else StudioColors.surface).border(
            1.dp,
            if (selected) StudioColors.accent else StudioColors.border,
            RoundedCornerShape(16.dp),
        ).clickable(enabled = enabled, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 3.dp).size(18.dp).clip(CircleShape).background(if (selected) StudioColors.accent else Color.Transparent).border(
                1.5.dp,
                if (selected) StudioColors.accent else StudioColors.textHint,
                CircleShape,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = label, style = bodyStyle(fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(text = if (enabled) note else "$note\n当前 Android 端不可用", style = secondaryStyle())
        }
    }
}

@Composable
private fun PresetRow(
    title: String,
    note: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) StudioColors.accentSoft else StudioColors.surface).border(
            1.dp,
            if (selected) StudioColors.accent else StudioColors.border,
            RoundedCornerShape(16.dp),
        ).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 3.dp).size(18.dp).clip(CircleShape).background(if (selected) StudioColors.accent else Color.Transparent).border(
                1.5.dp,
                if (selected) StudioColors.accent else StudioColors.textHint,
                CircleShape,
            ),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = title, style = bodyStyle(fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(text = note, style = secondaryStyle())
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    obscureText: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(text = label, style = bodyStyle(fontSize = 13.sp, color = StudioColors.textSecondary, fontWeight = FontWeight.Medium))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = StudioColors.textPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(StudioColors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (obscureText) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerField ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StudioColors.surfaceMuted).border(
                        1.dp,
                        if (error == null) StudioColors.border else StudioColors.danger,
                        RoundedCornerShape(16.dp),
                    ).padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    if (value.isBlank()) {
                        BasicText(text = placeholder, style = TextStyle(color = StudioColors.textHint, fontSize = 15.sp))
                    }
                    innerField()
                }
            },
        )
        error?.let { InlineError(text = it) }
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (enabled) StudioColors.accent else StudioColors.textHint).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = bodyStyle(color = Color.White, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun ToggleRow(title: String, note: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StudioColors.surfaceMuted).clickable { onToggle(!checked) }.padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(if (checked) StudioColors.accent else Color.White).border(1.dp, if (checked) StudioColors.accent else StudioColors.border, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                BasicText(text = "✓", style = bodyStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BasicText(text = title, style = bodyStyle(fontWeight = FontWeight.SemiBold))
            BasicText(text = note, style = bodyStyle(fontSize = 13.sp, color = StudioColors.textSecondary, lineHeight = 18.sp))
        }
    }
}

@Composable
private fun CircleIconButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(StudioColors.surfaceMuted).border(1.dp, StudioColors.border, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = TextStyle(color = StudioColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun InlineError(text: String) {
    BasicText(text = text, style = bodyStyle(color = StudioColors.danger, fontSize = 13.sp))
}

@Composable
private fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioColors.divider))
}

@Composable
private fun titleStyle(): TextStyle = TextStyle(color = StudioColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun secondaryStyle(): TextStyle = TextStyle(color = StudioColors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)

@Composable
private fun bodyStyle(
    color: Color = StudioColors.textPrimary,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = 22.sp,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = lineHeight,
    textAlign = TextAlign.Start,
)