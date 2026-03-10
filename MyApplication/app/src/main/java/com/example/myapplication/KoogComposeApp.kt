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
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

private object AppColors {
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

private enum class DemoRoute(val value: String) {
    Chat("chat"),
    Settings("settings"),
}

private enum class MessageRole {
    User,
    Assistant,
    System,
}

private data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String,
    val label: String? = null,
)

private const val STREAMING_PLACEHOLDER = "正在思考…"

private fun StoredChatMessage.toUiMessage(): ChatMessage = ChatMessage(
    id = id,
    role = runCatching { MessageRole.valueOf(role) }.getOrDefault(MessageRole.System),
    text = text,
    label = label,
)

private fun ChatMessage.toStoredMessage(): StoredChatMessage = StoredChatMessage(
    id = id,
    role = role.name,
    text = text,
    label = label,
)

private data class FormErrors(
    val provider: String? = null,
    val modelId: String? = null,
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val extraConfig: String? = null,
) {
    fun hasAny(): Boolean = listOf(provider, modelId, apiKey, baseUrl, extraConfig).any { it != null }
}

@Composable
fun KoogComposeApp() {
    val appContext = LocalContext.current.applicationContext
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val localStore = remember(appContext) { AppLocalStore(appContext) }
    val restoredState = remember(localStore) { localStore.loadState() }
    val restoredProvider = remember(restoredState.settings.providerName) {
        KoogProvider.entries.firstOrNull { it.name == restoredState.settings.providerName } ?: KoogProvider.OPENAI
    }
    val restoredMessages = remember(restoredState.messages) { restoredState.messages.map { it.toUiMessage() } }

    var drawerOpen by remember { mutableStateOf(false) }
    var providerName by rememberSaveable { mutableStateOf(restoredProvider.name) }
    var apiKey by rememberSaveable { mutableStateOf(restoredState.settings.apiKey) }
    var modelId by rememberSaveable { mutableStateOf(restoredState.settings.modelId) }
    var baseUrl by rememberSaveable { mutableStateOf(restoredState.settings.baseUrl) }
    var extraConfig by rememberSaveable { mutableStateOf(restoredState.settings.extraConfig) }
    var prompt by rememberSaveable { mutableStateOf(restoredState.settings.promptDraft) }
    var formErrors by remember { mutableStateOf(FormErrors()) }
    var isRunning by remember { mutableStateOf(false) }
    var nextMessageId by remember { mutableLongStateOf((restoredMessages.maxOfOrNull { it.id } ?: 0L) + 1L) }
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            addAll(restoredMessages)
        }
    }

    val provider = remember(providerName) { KoogProvider.valueOf(providerName) }

    fun persistSettings(
        providerNameValue: String = providerName,
        apiKeyValue: String = apiKey,
        modelIdValue: String = modelId,
        baseUrlValue: String = baseUrl,
        extraConfigValue: String = extraConfig,
        promptValue: String = prompt,
    ) {
        localStore.saveSettings(
            StoredSettings(
                providerName = providerNameValue,
                apiKey = apiKeyValue,
                modelId = modelIdValue,
                baseUrl = baseUrlValue,
                extraConfig = extraConfigValue,
                promptDraft = promptValue,
            )
        )
    }

    fun persistMessages() {
        localStore.saveMessages(messages.map(ChatMessage::toStoredMessage))
    }

    fun addMessage(role: MessageRole, text: String, label: String? = null): Long {
        val message = ChatMessage(id = nextMessageId++, role = role, text = text, label = label)
        messages += message
        persistMessages()
        return message.id
    }

    fun updateMessage(messageId: Long, update: (ChatMessage) -> ChatMessage) {
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
        formErrors = FormErrors()
        persistSettings(
            providerNameValue = next.name,
            modelIdValue = next.defaultModelId,
            baseUrlValue = next.defaultBaseUrl,
            extraConfigValue = next.extraFieldDefault,
        )
    }

    fun validateSettings(): FormErrors = FormErrors(
        provider = if (!provider.isSupportedOnAndroid) "当前 Android 版本暂不支持该供应商，请切换到其他供应商。" else null,
        modelId = if (modelId.isBlank()) "请输入模型 ID" else null,
        apiKey = if (provider.requiresApiKey && apiKey.isBlank()) "请先输入 API Key" else null,
        baseUrl = if ((provider == KoogProvider.AZURE_OPENAI || provider == KoogProvider.OLLAMA) && baseUrl.isBlank()) "该供应商需要 Base URL" else null,
        extraConfig = if (provider.extraFieldLabel != null && extraConfig.isBlank()) "请填写该供应商所需的额外配置" else null,
    )

    fun settingsSummary(errors: FormErrors): String = buildList {
        errors.provider?.let(::add)
        errors.modelId?.let(::add)
        errors.apiKey?.let(::add)
        errors.baseUrl?.let(::add)
        errors.extraConfig?.let(::add)
    }.joinToString("；")

    fun buildRequest(userPrompt: String): AgentRequest = AgentRequest(
        provider = provider,
        apiKey = apiKey.trim(),
        modelId = modelId.trim(),
        baseUrl = baseUrl.trim(),
        extraConfig = extraConfig.trim(),
        runtimePreset = AgentRuntimePreset.StreamingWithTools,
        systemPrompt = "",
        temperature = 0.2,
        maxIterations = 50,
        featureConfig = AgentFeatureConfig(
            localWriterEnabled = true,
            debuggerEnabled = false,
            debuggerPort = 50881,
            debuggerWaitMillis = 250,
            remoteClientEnabled = false,
            remoteHost = "127.0.0.1",
            remotePort = 50881,
        ),
        userPrompt = userPrompt.trim(),
    )

    fun submitPrompt() {
        val userPrompt = prompt.trim()
        if (userPrompt.isBlank() || isRunning) return

        val validation = validateSettings()
        formErrors = validation
        if (validation.hasAny()) {
            addMessage(
                role = MessageRole.System,
                label = "设置未完成",
                text = "当前还不能发送消息，请先到设置页完善配置：${settingsSummary(validation)}",
            )
            return
        }

        addMessage(role = MessageRole.User, text = userPrompt)
        val assistantMessageId = addMessage(
            role = MessageRole.Assistant,
            label = provider.displayName,
            text = STREAMING_PLACEHOLDER,
        )
        prompt = ""
        persistSettings(promptValue = "")
        isRunning = true

        scope.launch {
            try {
                val result = KoogAgentRunner.runAgentStreaming(
                    request = buildRequest(userPrompt),
                    onTextDelta = { delta ->
                        updateMessage(assistantMessageId) { current ->
                            val nextText = when {
                                current.text == STREAMING_PLACEHOLDER -> delta
                                else -> current.text + delta
                            }
                            current.copy(text = nextText)
                        }
                    }
                )
                updateMessage(assistantMessageId) { current ->
                    val mergedText = current.text.takeUnless { it.isBlank() || it == STREAMING_PLACEHOLDER }
                    current.copy(text = mergedText ?: result.answer)
                }
                if (result.events.isNotEmpty()) {
                    addMessage(role = MessageRole.System, label = "执行日志", text = result.events.joinToString("\n"))
                }
            } catch (error: Throwable) {
                removeMessage(assistantMessageId)
                val message = error.message ?: error::class.simpleName ?: "Unknown error"
                addMessage(role = MessageRole.System, label = "错误", text = message)
            } finally {
                isRunning = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.page)) {
        NavHost(navController = navController, startDestination = DemoRoute.Chat.value) {
            composable(DemoRoute.Chat.value) {
                ChatScreen(
                    provider = provider,
                    prompt = prompt,
                    isRunning = isRunning,
                    messages = messages,
                    drawerOpen = drawerOpen,
                    onPromptChanged = {
                        prompt = it
                        persistSettings(promptValue = it)
                    },
                    onSendClick = ::submitPrompt,
                    onMenuClick = { drawerOpen = true },
                    onCloseDrawer = { drawerOpen = false },
                    onOpenSettings = {
                        drawerOpen = false
                        navController.navigate(DemoRoute.Settings.value)
                    },
                    onClearChat = {
                        messages.clear()
                        persistMessages()
                        addMessage(role = MessageRole.System, label = "新对话", text = "对话已清空。现在可以重新开始聊天。")
                        drawerOpen = false
                    },
                )
            }

            composable(DemoRoute.Settings.value) {
                SettingsScreen(
                    provider = provider,
                    apiKey = apiKey,
                    modelId = modelId,
                    baseUrl = baseUrl,
                    extraConfig = extraConfig,
                    errors = formErrors,
                    onBackClick = { navController.popBackStack() },
                    onDoneClick = { navController.popBackStack() },
                    onProviderSelected = ::applyProvider,
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
                )
            }
        }
    }
}

@Composable
private fun ChatScreen(
    provider: KoogProvider,
    prompt: String,
    isRunning: Boolean,
    messages: List<ChatMessage>,
    drawerOpen: Boolean,
    onPromptChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onMenuClick: () -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit,
) {
    val listState = rememberLazyListState()
    val drawerOffset by animateDpAsState(if (drawerOpen) 0.dp else (-304).dp, animationSpec = tween(220), label = "drawer")
    val scrimAlpha by animateFloatAsState(if (drawerOpen) 0.28f else 0f, animationSpec = tween(220), label = "scrim")

    LaunchedEffect(messages.size, isRunning) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    BackHandler(enabled = drawerOpen, onBack = onCloseDrawer)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().background(AppColors.page)) {
            ChatTopBar(provider = provider, onMenuClick = onMenuClick)
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

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)).clickable(onClick = onCloseDrawer),
            )
        }

        SideDrawer(
            provider = provider,
            offsetX = drawerOffset,
            onCloseDrawer = onCloseDrawer,
            onOpenSettings = onOpenSettings,
            onClearChat = onClearChat,
        )
    }
}

@Composable
private fun SettingsScreen(
    provider: KoogProvider,
    apiKey: String,
    modelId: String,
    baseUrl: String,
    extraConfig: String,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
    onProviderSelected: (KoogProvider) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.page).statusBarsPadding()) {
        SettingsTopBar(onBackClick = onBackClick, onDoneClick = onDoneClick)
        DividerLine()

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsSection(title = "供应商", subtitle = "选择当前聊天要使用的 LLM 提供方") {
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

            SettingsSection(title = "模型与连接", subtitle = "这里的配置会直接影响聊天页发送请求") {
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
        }
    }
}

@Composable
private fun ChatTopBar(provider: KoogProvider, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AppColors.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(text = "☰", onClick = onMenuClick)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = "Koog Chat", style = titleStyle())
            BasicText(text = "当前供应商：${provider.displayName}", style = secondaryStyle())
        }
    }
}

@Composable
private fun SettingsTopBar(onBackClick: () -> Unit, onDoneClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(AppColors.surface).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(text = "←", onClick = onBackClick)
        Spacer(modifier = Modifier.width(12.dp))
        BasicText(text = "设置", style = titleStyle(), modifier = Modifier.weight(1f))
        TextActionButton(text = "完成", onClick = onDoneClick)
    }
}

@Composable
private fun SideDrawer(
    provider: KoogProvider,
    offsetX: androidx.compose.ui.unit.Dp,
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearChat: () -> Unit,
) {
    Column(
        modifier = Modifier.statusBarsPadding().offset(x = offsetX).fillMaxHeight().width(304.dp).background(AppColors.surface).border(1.dp, AppColors.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = "Koog Chat", style = titleStyle())
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(text = provider.displayName, style = secondaryStyle())
            }
            CircleIconButton(text = "×", onClick = onCloseDrawer)
        }

        DividerLine()
        DrawerItem(title = "清空当前对话", onClick = onClearChat)
        Spacer(modifier = Modifier.weight(1f))
        DividerLine()
        DrawerItem(title = "设置", onClick = onOpenSettings, bottom = true)
    }
}

@Composable
private fun DrawerItem(title: String, onClick: () -> Unit, bottom: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = if (bottom) 18.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text = title, style = bodyStyle())
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when (message.role) {
        MessageRole.User -> AppColors.userBubble
        MessageRole.Assistant -> AppColors.assistantBubble
        MessageRole.System -> AppColors.systemBubble
    }
    val textColor = if (isUser) AppColors.userBubbleText else AppColors.textPrimary

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier.fillMaxWidth(0.82f).clip(RoundedCornerShape(20.dp)).background(bubbleColor).border(
                1.dp,
                if (isUser) AppColors.userBubble else AppColors.border,
                RoundedCornerShape(20.dp),
            ).padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            message.label?.let {
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = if (isUser) AppColors.userBubbleText.copy(alpha = 0.88f) else AppColors.textSecondary,
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
private fun TypingBubble() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(AppColors.assistantBubble).border(
                1.dp,
                AppColors.border,
                RoundedCornerShape(18.dp),
            ).padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppColors.textHint))
            }
            Spacer(modifier = Modifier.width(4.dp))
            BasicText(text = "正在思考…", style = secondaryStyle())
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
        modifier = Modifier.fillMaxWidth().background(AppColors.surface).navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = TextStyle(color = AppColors.textPrimary, fontSize = 15.sp, lineHeight = 21.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            cursorBrush = SolidColor(AppColors.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { innerField ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppColors.surfaceMuted).border(
                        1.dp,
                        AppColors.border,
                        RoundedCornerShape(24.dp),
                    ).padding(horizontal = 16.dp, vertical = 14.dp).heightIn(min = 52.dp),
                ) {
                    if (value.isBlank()) {
                        BasicText(text = "输入消息，供应商配置在设置页", style = TextStyle(color = AppColors.textHint, fontSize = 15.sp))
                    }
                    innerField()
                }
            },
        )

        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(if (enabled) AppColors.accent else AppColors.textHint).clickable(
                enabled = enabled,
                onClick = onSendClick,
            ),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "发", style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AppColors.surface).border(
            1.dp,
            AppColors.border,
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
private fun ProviderRow(
    label: String,
    note: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) AppColors.accentSoft else AppColors.surface).border(
            1.dp,
            if (selected) AppColors.accent else AppColors.border,
            RoundedCornerShape(16.dp),
        ).clickable(enabled = enabled, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 3.dp).size(18.dp).clip(CircleShape).background(if (selected) AppColors.accent else Color.Transparent).border(
                1.5.dp,
                if (selected) AppColors.accent else AppColors.textHint,
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
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    obscureText: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(text = label, style = bodyStyle(fontSize = 13.sp, color = AppColors.textSecondary, fontWeight = FontWeight.Medium))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = AppColors.textPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(AppColors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (obscureText) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerField ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppColors.surfaceMuted).border(
                        1.dp,
                        if (error == null) AppColors.border else AppColors.danger,
                        RoundedCornerShape(16.dp),
                    ).padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    if (value.isBlank()) {
                        BasicText(text = placeholder, style = TextStyle(color = AppColors.textHint, fontSize = 15.sp))
                    }
                    innerField()
                }
            },
        )
        error?.let { InlineError(text = it) }
    }
}

@Composable
private fun CircleIconButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.surfaceMuted).border(1.dp, AppColors.border, CircleShape).clickable(
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text = text, style = TextStyle(color = AppColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun TextActionButton(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)) {
        BasicText(text = text, style = bodyStyle(color = AppColors.accent, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun InlineError(text: String) {
    BasicText(text = text, style = bodyStyle(color = AppColors.danger, fontSize = 13.sp))
}

@Composable
private fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.divider))
}

@Composable
private fun titleStyle(): TextStyle = TextStyle(color = AppColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun secondaryStyle(): TextStyle = TextStyle(color = AppColors.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)

@Composable
private fun bodyStyle(
    color: Color = AppColors.textPrimary,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Normal,
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    lineHeight = 22.sp,
    textAlign = TextAlign.Start,
)