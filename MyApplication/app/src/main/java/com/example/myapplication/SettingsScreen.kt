package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.components.DropdownField
import com.example.myapplication.components.Icon
import com.example.myapplication.components.IconButton
import com.example.myapplication.components.Scaffold
import com.example.myapplication.components.Surface
import com.example.myapplication.components.Text
import com.example.myapplication.components.TopAppBar

@Composable
internal fun SettingsHomeScreen(
    state: SettingsState,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onOpenProviderSettings: () -> Unit,
    onOpenRuntimeSettings: () -> Unit,
    onProviderSelected: (Provider) -> Unit,
    onRuntimePresetSelected: (AgentRuntimePreset) -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "设置",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    backgroundColor = Color.White,
                    modifier = Modifier.height(64.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            if (errors.hasAny()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠️ ${settingsSummary(errors)}",
                        fontSize = 14.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DropdownField(
                    label = "模型供应商",
                    value = state.provider.displayName,
                    items = Provider.entries.filter { it.isSupportedOnAndroid },
                    itemLabel = { it.displayName },
                    onItemSelected = { provider ->
                        onProviderSelected(provider)
                        onOpenProviderSettings()
                    }
                )
                
                DropdownField(
                    label = "运行模式",
                    value = state.runtimePreset.title,
                    items = AgentRuntimePreset.entries,
                    itemLabel = { it.title },
                    onItemSelected = { preset ->
                        onRuntimePresetSelected(preset)
                        onOpenRuntimeSettings()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun ProviderSettingsScreen(
    state: SettingsState,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Text(
                            state.provider.displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    backgroundColor = Color.White,
                    modifier = Modifier.height(64.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    "模型 ID",
                    state.modelId,
                    onModelIdChanged,
                    errors.modelId,
                    state.provider.defaultModelId
                )
                
                if (state.provider.requiresApiKey) {
                    TextField(
                        "API Key",
                        state.apiKey,
                        onApiKeyChanged,
                        errors.apiKey,
                        "输入 API 密钥",
                        secure = true
                    )
                }
                
                if (state.provider != Provider.BEDROCK) {
                    TextField(
                        state.provider.baseUrlLabel,
                        state.baseUrl,
                        onBaseUrlChanged,
                        errors.baseUrl,
                        state.provider.defaultBaseUrl.ifBlank { "https://api.example.com" },
                        keyboardType = KeyboardType.Uri
                    )
                }
                
                state.provider.extraFieldLabel?.let {
                    TextField(
                        it,
                        state.extraConfig,
                        onExtraConfigChanged,
                        errors.extraConfig,
                        state.provider.extraFieldDefault
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun RuntimeSettingsScreen(
    state: SettingsState,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onSystemPromptChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxIterationsChanged: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Text(
                            state.runtimePreset.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Color(0xFF333333),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    backgroundColor = Color.White,
                    modifier = Modifier.height(64.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    "System Prompt",
                    state.systemPrompt,
                    onSystemPromptChanged,
                    placeholder = "可选的系统提示词",
                    singleLine = false
                )
                
                TextField(
                    "Temperature",
                    state.temperature,
                    onTemperatureChanged,
                    errors.temperature,
                    "0.2",
                    keyboardType = KeyboardType.Decimal
                )
                
                TextField(
                    "Max Iterations",
                    state.maxIterations,
                    onMaxIterationsChanged,
                    errors.maxIterations,
                    "50",
                    keyboardType = KeyboardType.Number
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

