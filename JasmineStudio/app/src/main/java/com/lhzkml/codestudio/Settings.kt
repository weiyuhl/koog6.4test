package com.lhzkml.codestudio

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
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.DropdownField
import com.lhzkml.codestudio.components.Icon
import com.lhzkml.codestudio.components.IconButton
import com.lhzkml.codestudio.components.Text

@Composable
internal fun SettingsHomeScreen(
    state: State,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onOpenProvider: () -> Unit,
    onOpenRuntime: () -> Unit,
    onProviderChange: (Provider) -> Unit,
    onRuntimeChange: (Preset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = "设置",
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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
                        onProviderChange(provider)
                        onOpenProvider()
                    }
                )
                
                DropdownField(
                    label = "运行模式",
                    value = state.runtimePreset.title,
                    items = Preset.entries,
                    itemLabel = { it.title },
                    onItemSelected = { preset ->
                        onRuntimeChange(preset)
                        onOpenRuntime()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun ProviderSettingsScreen(
    state: State,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = state.provider.displayName,
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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
    state: State,
    errors: FormErrors,
    onBackClick: () -> Unit,
    onSystemPromptChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxIterationsChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = state.runtimePreset.title,
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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


