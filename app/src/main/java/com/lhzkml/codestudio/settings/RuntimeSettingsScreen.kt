package com.lhzkml.codestudio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lhzkml.codestudio.TextField
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.ui.model.RuntimeSettingsUiModel

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
