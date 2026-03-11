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
import com.lhzkml.codestudio.viewmodel.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
internal fun App() {
    val context = LocalContext.current
    val factory = ViewModelFactory(context)
    
    val navigationViewModel: NavigationViewModel = viewModel(factory = factory)
    val chatViewModel: ChatViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    
    val navigationState by navigationViewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val sideState = rememberSideState(SideValue.Closed)

    when (navigationState.currentRoute) {
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
                                        navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Chat.value))
                                        scope.launch { sideState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )

                                SideItem(
                                    icon = { Text("🗑️") },
                                    label = { Text("清空对话", fontSize = 16.sp) },
                                    selected = false,
                                    onClick = {
                                        chatViewModel.onEvent(ChatEvent.ClearChat)
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
                                        "消息: ${chatState.messages.size}",
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        chatState.provider.displayName,
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                }

                                SideItem(
                                    icon = { Text("⚙️") },
                                    label = { Text("设置", fontSize = 16.sp) },
                                    selected = false,
                                    onClick = {
                                        navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Home.value))
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
                    provider = chatState.provider,
                    prompt = chatState.prompt,
                    isRunning = chatState.isRunning,
                    messages = chatState.messages,
                    onPromptChanged = { chatViewModel.onEvent(ChatEvent.UpdatePrompt(it)) },
                    onSendClick = { chatViewModel.onEvent(ChatEvent.SendMessage) },
                    onMenuClick = { scope.launch { sideState.open() } },
                )
            }
        }

        Route.Home.value -> {
            SettingsHomeScreen(
                state = settingsViewModel.toState(),
                errors = settingsState.formErrors,
                onBackClick = { navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Chat.value)) },
                onOpenProvider = { navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Model.value)) },
                onOpenRuntime = { navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Runtime.value)) },
                onProviderChange = { settingsViewModel.onEvent(SettingsEvent.UpdateProvider(it)) },
                onRuntimeChange = { settingsViewModel.onEvent(SettingsEvent.UpdateRuntimePreset(it)) },
            )
        }

        Route.Model.value -> {
            ProviderSettingsScreen(
                state = settingsViewModel.toState(),
                errors = settingsState.formErrors,
                onBackClick = { navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Home.value)) },
                onApiKeyChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateApiKey(it)) },
                onModelIdChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateModelId(it)) },
                onBaseUrlChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateBaseUrl(it)) },
                onExtraConfigChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateExtraConfig(it)) },
            )
        }

        Route.Runtime.value -> {
            RuntimeSettingsScreen(
                state = settingsViewModel.toState(),
                errors = settingsState.formErrors,
                onBackClick = { navigationViewModel.onEvent(NavigationEvent.NavigateTo(Route.Home.value)) },
                onSystemPromptChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateSystemPrompt(it)) },
                onTemperatureChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateTemperature(it)) },
                onMaxIterationsChanged = { settingsViewModel.onEvent(SettingsEvent.UpdateMaxIterations(it)) },
            )
        }
    }
}

