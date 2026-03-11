package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.graphics.Color
import com.lhzkml.codestudio.components.Side
import com.lhzkml.codestudio.components.SideItem
import com.lhzkml.codestudio.components.Text
import com.lhzkml.codestudio.components.rememberSideState
import com.lhzkml.codestudio.components.SideValue
import com.lhzkml.codestudio.viewmodel.MainViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
internal fun App(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val sideState = rememberSideState(SideValue.Closed)
    
    LaunchedEffect(uiState.currentRoute) {
        if (sideState.isOpen) sideState.close()
    }
    
    LaunchedEffect(uiState.isSidebarOpen) {
        if (uiState.isSidebarOpen) {
            sideState.open()
        } else {
            sideState.close()
        }
    }

    when (uiState.currentRoute) {
        Route.Chat.value -> {
            Side(
                sideState = sideState,
                sideContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .statusBarsPadding()
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Colors.Surface)
                        ) {
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
                                    selected = true,
                                    onClick = {
                                        viewModel.navigateTo(Route.Chat.value)
                                        scope.launch { sideState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )

                                SideItem(
                                    icon = { Text("🗑️") },
                                    label = { Text("清空对话", fontSize = 16.sp) },
                                    selected = false,
                                    onClick = {
                                        viewModel.clearChat()
                                        scope.launch { sideState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Column(
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "消息: ${uiState.messages.size}",
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        uiState.provider.displayName,
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                }

                                SideItem(
                                    icon = { Text("⚙️") },
                                    label = { Text("设置", fontSize = 16.sp) },
                                    selected = false,
                                    onClick = {
                                        viewModel.navigateTo(Route.Home.value)
                                        scope.launch { sideState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
            ) {
                ChatScreen(
                    provider = uiState.provider,
                    prompt = uiState.prompt,
                    isRunning = uiState.isRunning,
                    messages = uiState.messages,
                    onPromptChanged = viewModel::updatePrompt,
                    onSendClick = viewModel::submitPrompt,
                    onMenuClick = { scope.launch { sideState.open() } },
                )
            }
        }

        Route.Home.value -> {
            SettingsHomeScreen(
                state = State(
                    provider = uiState.provider,
                    apiKey = uiState.apiKey,
                    modelId = uiState.modelId,
                    baseUrl = uiState.baseUrl,
                    extraConfig = uiState.extraConfig,
                    promptDraft = uiState.prompt,
                    runtimePreset = uiState.runtimePreset,
                    systemPrompt = uiState.systemPrompt,
                    temperature = uiState.temperature,
                    maxIterations = uiState.maxIterations
                ),
                errors = uiState.formErrors,
                onBackClick = { viewModel.navigateTo(Route.Chat.value) },
                onOpenProvider = { viewModel.navigateTo(Route.Model.value) },
                onOpenRuntime = { viewModel.navigateTo(Route.Runtime.value) },
                onProviderChange = viewModel::updateProvider,
                onRuntimeChange = viewModel::updateRuntimePreset,
            )
        }

        Route.Model.value -> {
            ProviderSettingsScreen(
                state = State(
                    provider = uiState.provider,
                    apiKey = uiState.apiKey,
                    modelId = uiState.modelId,
                    baseUrl = uiState.baseUrl,
                    extraConfig = uiState.extraConfig,
                    promptDraft = uiState.prompt,
                    runtimePreset = uiState.runtimePreset,
                    systemPrompt = uiState.systemPrompt,
                    temperature = uiState.temperature,
                    maxIterations = uiState.maxIterations
                ),
                errors = uiState.formErrors,
                onBackClick = { viewModel.navigateTo(Route.Home.value) },
                onApiKeyChanged = viewModel::updateApiKey,
                onModelIdChanged = viewModel::updateModelId,
                onBaseUrlChanged = viewModel::updateBaseUrl,
                onExtraConfigChanged = viewModel::updateExtraConfig,
            )
        }

        Route.Runtime.value -> {
            RuntimeSettingsScreen(
                state = State(
                    provider = uiState.provider,
                    apiKey = uiState.apiKey,
                    modelId = uiState.modelId,
                    baseUrl = uiState.baseUrl,
                    extraConfig = uiState.extraConfig,
                    promptDraft = uiState.prompt,
                    runtimePreset = uiState.runtimePreset,
                    systemPrompt = uiState.systemPrompt,
                    temperature = uiState.temperature,
                    maxIterations = uiState.maxIterations
                ),
                errors = uiState.formErrors,
                onBackClick = { viewModel.navigateTo(Route.Home.value) },
                onSystemPromptChanged = viewModel::updateSystemPrompt,
                onTemperatureChanged = viewModel::updateTemperature,
                onMaxIterationsChanged = viewModel::updateMaxIterations,
            )
        }
    }
}

