package com.example.myapplication

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
import com.example.myapplication.components.DrawerSheet
import com.example.myapplication.components.Icon
import com.example.myapplication.components.NavigationDrawer
import com.example.myapplication.components.NavigationDrawerItem
import com.example.myapplication.components.Text
import com.example.myapplication.components.rememberDrawerState
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@Composable
fun App() {
    val appContext = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(false)
    val localStore = remember(appContext) { AppLocalStore(appContext) }
    val restoredState = remember(localStore) { localStore.loadState() }
    val restoredProvider = remember(restoredState.settings.providerName) { 
        Provider.entries.firstOrNull { it.name == restoredState.settings.providerName } ?: Provider.OPENAI 
    }
    val restoredPreset = remember(localStore) { AgentRuntimePreset.fromId(localStore.loadRuntimePresetId()) }
    val restoredMessages = remember(restoredState.messages) { restoredState.messages.map { it.toNativeMessage() } }

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
    var formErrors by remember { mutableStateOf(NativeFormErrors()) }
    var isRunning by remember { mutableStateOf(false) }
    var nextMessageId by remember { mutableLongStateOf((restoredMessages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val messages = remember { mutableStateListOf<NativeChatMessage>().apply { addAll(restoredMessages) } }
    val provider = remember(providerName) { Provider.valueOf(providerName) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NativeRoute.Chat.value

    fun currentState(
        providerValue: Provider = provider,
        apiKeyValue: String = apiKey,
        modelIdValue: String = modelId,
        baseUrlValue: String = baseUrl,
        extraConfigValue: String = extraConfig,
        promptValue: String = prompt,
        runtimePresetValue: AgentRuntimePreset = runtimePreset,
        systemPromptValue: String = systemPrompt,
        temperatureValue: String = temperature,
        maxIterationsValue: String = maxIterations
    ) = NativeSettingsState(
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

    fun persistSettings(state: NativeSettingsState = currentState()) {
        localStore.saveSettings(state.toStoredSettings())
        localStore.saveRuntimePresetId(state.runtimePreset.id)
    }

    fun persistMessages() {
        localStore.saveMessages(messages.map(NativeChatMessage::toStoredMessage))
    }

    fun addMessage(role: NativeMessageRole, text: String, label: String? = null): Long {
        val message = NativeChatMessage(nextMessageId++, role, text, label)
        messages += message
        persistMessages()
        return message.id
    }

    fun updateMessage(id: Long, update: (NativeChatMessage) -> NativeChatMessage) {
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
        addMessage(NativeMessageRole.System, "对话已清空。现在可以重新开始聊天。", "新对话")
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
        }
    }

    fun openSettings() {
        scope.launch { drawerState.close() }
        navigateTo(NativeRoute.SettingsHome.value)
    }

    fun openChat() {
        scope.launch { drawerState.close() }
        navigateTo(NativeRoute.Chat.value)
    }

    fun submitPrompt() {
        val userPrompt = prompt.trim()
        if (userPrompt.isBlank() || isRunning) return
        val validation = validateNativeSettings(currentState())
        formErrors = validation
        if (validation.hasAny()) {
            addMessage(
                NativeMessageRole.System,
                "当前还不能发送消息，请先到设置页完善配置：${nativeSettingsSummary(validation)}",
                "设置未完成"
            )
            openSettings()
            return
        }
        addMessage(NativeMessageRole.User, userPrompt)
        val assistantId = addMessage(NativeMessageRole.Assistant, NATIVE_STREAMING_PLACEHOLDER, provider.displayName)
        prompt = ""
        persistSettings(currentState(promptValue = ""))
        isRunning = true
        scope.launch {
            try {
                val result = KoogAgentRunner.runAgentStreaming(
                    request = currentState(promptValue = "").toAgentRequest(userPrompt),
                    onTextDelta = { delta ->
                        updateMessage(assistantId) { current ->
                            current.copy(
                                text = if (current.text == NATIVE_STREAMING_PLACEHOLDER) delta else current.text + delta
                            )
                        }
                    }
                )
                updateMessage(assistantId) { current ->
                    current.copy(
                        text = current.text.takeUnless { it.isBlank() || it == NATIVE_STREAMING_PLACEHOLDER }
                            ?: result.answer
                    )
                }
                if (result.events.isNotEmpty()) {
                    addMessage(NativeMessageRole.System, result.events.joinToString("\n"), "执行日志")
                }
            } catch (error: Throwable) {
                removeMessage(assistantId)
                addMessage(
                    NativeMessageRole.System,
                    error.message ?: error::class.simpleName ?: "Unknown error",
                    "错误"
                )
            } finally {
                isRunning = false
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (drawerState.isOpen) drawerState.close()
    }

    NavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top section
                    Column {
                        Text(
                            "Koog",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(20.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Text("💬") },
                            label = { Text("聊天") },
                            selected = currentRoute == NativeRoute.Chat.value,
                            onClick = ::openChat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            label = { Text("清空对话") },
                            selected = false,
                            onClick = {
                                clearChat()
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Bottom section
                    Column {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "消息: ${messages.size}",
                                fontSize = 13.sp,
                                color = Color(0xFF999999)
                            )
                            Text(
                                provider.displayName,
                                fontSize = 13.sp,
                                color = Color(0xFF999999)
                            )
                        }

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text("设置") },
                            selected = currentRoute != NativeRoute.Chat.value,
                            onClick = ::openSettings,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = NativeRoute.Chat.value,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(NativeRoute.Chat.value) {
                NativeChatScreen(
                    provider = provider,
                    prompt = prompt,
                    isRunning = isRunning,
                    messages = messages,
                    onPromptChanged = { prompt = it; persistSettings(currentState(promptValue = it)) },
                    onSendClick = ::submitPrompt,
                    onMenuClick = { scope.launch { drawerState.open() } },
                )
            }

            composable(NativeRoute.SettingsHome.value) {
                NativeSettingsHomeScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { if (!navController.popBackStack()) openChat() },
                    onOpenProviderSettings = {
                        navController.navigate(NativeRoute.SettingsModel.value) { launchSingleTop = true }
                    },
                    onOpenRuntimeSettings = {
                        navController.navigate(NativeRoute.SettingsRuntime.value) { launchSingleTop = true }
                    },
                    onProviderSelected = { next ->
                        providerName = next.name
                        modelId = next.defaultModelId
                        baseUrl = next.defaultBaseUrl
                        extraConfig = next.extraFieldDefault
                        formErrors = NativeFormErrors()
                        persistSettings(
                            currentState(
                                providerValue = next,
                                modelIdValue = next.defaultModelId,
                                baseUrlValue = next.defaultBaseUrl,
                                extraConfigValue = next.extraFieldDefault
                            )
                        )
                    },
                    onRuntimePresetSelected = { next ->
                        runtimePreset = next
                        persistSettings(currentState(runtimePresetValue = next))
                    },
                )
            }

            composable(NativeRoute.SettingsModel.value) {
                NativeProviderSettingsScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { navController.popBackStack() },
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

            composable(NativeRoute.SettingsRuntime.value) {
                NativeRuntimeSettingsScreen(
                    state = currentState(),
                    errors = formErrors,
                    onBackClick = { navController.popBackStack() },
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
