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
import com.lhzkml.codestudio.components.Text
import com.lhzkml.codestudio.ui.model.SettingsHomeUiModel
import com.lhzkml.codestudio.ui.model.ProviderSettingsUiModel
import com.lhzkml.codestudio.ui.model.RuntimeSettingsUiModel

@Composable
internal fun SettingsHomeScreen(
    uiModel: SettingsHomeUiModel,
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
            if (uiModel.errors.hasAny()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠️ ${settingsSummary(uiModel.errors)}",
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
                    value = uiModel.providerDisplayName,
                    items = Provider.entries.filter { it.isSupportedOnAndroid },
                    itemLabel = { it.displayName },
                    onItemSelected = { provider ->
                        onProviderChange(provider)
                        onOpenProvider()
                    }
                )
                
                DropdownField(
                    label = "运行模式",
                    value = uiModel.runtimePresetTitle,
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
    uiModel: ProviderSettingsUiModel,
    onBackClick: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = uiModel.providerDisplayName,
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
                    uiModel.modelId,
                    onModelIdChanged,
                    uiModel.errors.modelId,
                    uiModel.modelIdPlaceholder
                )
                
                if (uiModel.showApiKey) {
                    TextField(
                        "API Key",
                        uiModel.apiKey,
                        onApiKeyChanged,
                        uiModel.errors.apiKey,
                        uiModel.apiKeyPlaceholder,
                        secure = true
                    )
                }
                
                if (uiModel.showBaseUrl) {
                    TextField(
                        uiModel.baseUrlLabel,
                        uiModel.baseUrl,
                        onBaseUrlChanged,
                        uiModel.errors.baseUrl,
                        uiModel.baseUrlPlaceholder,
                        keyboardType = KeyboardType.Uri
                    )
                }
                
                uiModel.extraFieldLabel?.let { label ->
                    if (uiModel.showExtraField) {
                        TextField(
                            label,
                            uiModel.extraConfig,
                            onExtraConfigChanged,
                            uiModel.errors.extraConfig,
                            uiModel.extraFieldPlaceholder
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun RuntimeSettingsScreen(
    uiModel: RuntimeSettingsUiModel,
    onBackClick: () -> Unit,
    onSystemPromptChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxIterationsChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = uiModel.runtimePresetTitle,
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
                    uiModel.systemPrompt,
                    onSystemPromptChanged,
                    placeholder = uiModel.systemPromptPlaceholder,
                    singleLine = false
                )
                
                TextField(
                    "Temperature",
                    uiModel.temperature,
                    onTemperatureChanged,
                    uiModel.errors.temperature,
                    uiModel.temperaturePlaceholder,
                    keyboardType = KeyboardType.Decimal
                )
                
                TextField(
                    "Max Iterations",
                    uiModel.maxIterations,
                    onMaxIterationsChanged,
                    uiModel.errors.maxIterations,
                    uiModel.maxIterationsPlaceholder,
                    keyboardType = KeyboardType.Number
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


