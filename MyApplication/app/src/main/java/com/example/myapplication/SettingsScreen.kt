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
import com.example.myapplication.components.Icon
import com.example.myapplication.components.IconButton
import com.example.myapplication.components.RadioButton
import com.example.myapplication.components.Scaffold
import com.example.myapplication.components.Surface
import com.example.myapplication.components.Text
import com.example.myapplication.components.TopAppBar

@Composable
internal fun NativeSettingsHomeScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onOpenProviderSettings: () -> Unit,
    onOpenRuntimeSettings: () -> Unit,
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
                        "⚠️ ${nativeSettingsSummary(errors)}",
                        fontSize = 14.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                title = "模型供应商",
                subtitle = "${state.provider.displayName} · ${state.modelId.ifBlank { "未设置" }}",
                onClick = onOpenProviderSettings
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(
                title = "运行模式",
                subtitle = state.runtimePreset.title,
                onClick = onOpenRuntimeSettings
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsItem(
                title = "Temperature",
                subtitle = state.temperature,
                onClick = onOpenRuntimeSettings
            )
        }
    }
}

@Composable
internal fun NativeProviderSettingsScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onProviderSelected: (Provider) -> Unit,
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
                            "模型供应商",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                Provider.entries.forEach { provider ->
                    ProviderItem(
                        provider = provider,
                        selected = provider == state.provider,
                        onSelect = { onProviderSelected(provider) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NativeTextField(
                    "模型 ID",
                    state.modelId,
                    onModelIdChanged,
                    errors.modelId,
                    state.provider.defaultModelId
                )
                
                if (state.provider.requiresApiKey) {
                    NativeTextField(
                        "API Key",
                        state.apiKey,
                        onApiKeyChanged,
                        errors.apiKey,
                        "输入 API 密钥",
                        secure = true
                    )
                }
                
                if (state.provider != Provider.BEDROCK) {
                    NativeTextField(
                        state.provider.baseUrlLabel,
                        state.baseUrl,
                        onBaseUrlChanged,
                        errors.baseUrl,
                        state.provider.defaultBaseUrl.ifBlank { "https://api.example.com" },
                        keyboardType = KeyboardType.Uri
                    )
                }
                
                state.provider.extraFieldLabel?.let {
                    NativeTextField(
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
internal fun NativeRuntimeSettingsScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onRuntimePresetSelected: (AgentRuntimePreset) -> Unit,
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
                            "运行参数",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                AgentRuntimePreset.entries.forEach { preset ->
                    PresetItem(
                        preset = preset,
                        selected = preset == state.runtimePreset,
                        onSelect = { onRuntimePresetSelected(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NativeTextField(
                    "System Prompt",
                    state.systemPrompt,
                    onSystemPromptChanged,
                    placeholder = "可选的系统提示词",
                    singleLine = false
                )
                
                NativeTextField(
                    "Temperature",
                    state.temperature,
                    onTemperatureChanged,
                    errors.temperature,
                    "0.2",
                    keyboardType = KeyboardType.Decimal
                )
                
                NativeTextField(
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

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = Color(0xFF999999)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ProviderItem(
    provider: Provider,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect, enabled = provider.isSupportedOnAndroid)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = provider.isSupportedOnAndroid,
            selectedColor = Color(0xFF007AFF),
            unselectedColor = Color(0xFFCCCCCC)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                provider.displayName,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = Color(0xFF333333)
            )
            Text(
                if (provider.isSupportedOnAndroid) provider.providerNote else "当前不可用",
                fontSize = 13.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun PresetItem(
    preset: AgentRuntimePreset,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            selectedColor = Color(0xFF007AFF),
            unselectedColor = Color(0xFFCCCCCC)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    preset.title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = Color(0xFF333333)
                )
                Text(
                    preset.family,
                    fontSize = 12.sp,
                    color = Color(0xFF007AFF)
                )
            }
            Text(
                preset.description,
                fontSize = 13.sp,
                color = Color(0xFF999999)
            )
        }
    }
}
