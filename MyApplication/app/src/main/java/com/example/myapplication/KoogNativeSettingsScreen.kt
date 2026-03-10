package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeSettingsHomeScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onOpenProviderSettings: () -> Unit,
    onOpenRuntimeSettings: () -> Unit,
    onOpenLocalToolsSettings: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("设置") },
            navigationIcon = { IconButton(onClick = onBackClick) { Text("←") } },
        )
    }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            NativeSettingsEntryItem("模型与供应商", "${state.provider.displayName} · ${state.modelId.ifBlank { "未设置模型" }}", onOpenProviderSettings)
            NativeSettingsEntryItem("运行参数", "${state.runtimePreset.title} · temperature ${state.temperature} · iterations ${state.maxIterations}", onOpenRuntimeSettings)
            NativeSettingsEntryItem("本地工具", "${if (state.codeToolsEnabled) "已启用" else "已关闭"} · ${state.codeToolsWorkspaceRoot.ifBlank { "未设置工作区" }}", onOpenLocalToolsSettings)
            if (errors.hasAny()) {
                NativeSectionCard(title = "当前需要处理", subtitle = "这些问题会影响聊天请求或工具执行") {
                    Text(nativeSettingsSummary(errors), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeProviderSettingsScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onProviderSelected: (KoogProvider) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
) {
    SettingsDetailScaffold(title = "模型与供应商", onBackClick = onBackClick) {
        NativeSectionCard(title = "供应商", subtitle = "选择当前聊天使用的模型供应商") {
            KoogProvider.entries.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = item == state.provider, onClick = { onProviderSelected(item) }, enabled = item.isSupportedOnAndroid)
                    Column {
                        Text(item.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(if (item.isSupportedOnAndroid) item.providerNote else "${item.providerNote}（当前 Android 端不可用）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            errors.provider?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        NativeSectionCard(title = "连接配置", subtitle = "只保留与当前模型连接相关的配置") {
            NativeTextField("模型 ID", state.modelId, onModelIdChanged, errors.modelId, state.provider.defaultModelId)
            if (state.provider.requiresApiKey) NativeTextField("API Key", state.apiKey, onApiKeyChanged, errors.apiKey, "输入供应商密钥", secure = true)
            if (state.provider != KoogProvider.BEDROCK) NativeTextField(state.provider.baseUrlLabel, state.baseUrl, onBaseUrlChanged, errors.baseUrl, state.provider.defaultBaseUrl.ifBlank { "https://example.com" }, keyboardType = KeyboardType.Uri)
            state.provider.extraFieldLabel?.let { NativeTextField(it, state.extraConfig, onExtraConfigChanged, errors.extraConfig, state.provider.extraFieldDefault) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    SettingsDetailScaffold(title = "运行参数", onBackClick = onBackClick) {
        NativeSectionCard(title = "运行模式", subtitle = "选择当前 Android App 运行 agent 的方式") {
            AgentRuntimePreset.entries.forEach { preset ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = preset == state.runtimePreset, onClick = { onRuntimePresetSelected(preset) })
                    Column {
                        Text(preset.title, style = MaterialTheme.typography.titleSmall)
                        Text("${preset.family} · ${preset.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        NativeSectionCard(title = "请求参数", subtitle = "这些参数会直接影响模型输出") {
            NativeTextField("System prompt", state.systemPrompt, onSystemPromptChanged, placeholder = "可选", singleLine = false)
            NativeTextField("Temperature", state.temperature, onTemperatureChanged, errors.temperature, "0.2", keyboardType = KeyboardType.Decimal)
            NativeTextField("Max iterations", state.maxIterations, onMaxIterationsChanged, errors.maxIterations, "50", keyboardType = KeyboardType.Number)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeLocalToolsSettingsScreen(
    state: NativeSettingsState,
    errors: NativeFormErrors,
    onBackClick: () -> Unit,
    onCodeToolsEnabledChanged: (Boolean) -> Unit,
    onCodeToolsWorkspaceRootChanged: (String) -> Unit,
    onCodeToolsAllowedPathPrefixesChanged: (String) -> Unit,
) {
    SettingsDetailScaffold(title = "本地工具", onBackClick = onBackClick) {
        NativeSectionCard(title = "工具开关", subtitle = "控制 Android 本地文件工具的可用状态") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text("启用本地工具", style = MaterialTheme.typography.titleSmall)
                    Text("关闭后工具页仍可进入，但不会执行文件操作。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = state.codeToolsEnabled, onCheckedChange = onCodeToolsEnabledChanged)
            }
        }
        NativeSectionCard(title = "访问范围", subtitle = "限制工具可访问的本地目录") {
            NativeTextField("Workspace root", state.codeToolsWorkspaceRoot, onCodeToolsWorkspaceRootChanged, errors.codeToolsWorkspaceRoot, "/data/user/0/.../files")
            NativeTextField("Allowed path prefixes", state.codeToolsAllowedPathPrefixes, onCodeToolsAllowedPathPrefixesChanged, placeholder = "多个路径用分号分隔", singleLine = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDetailScaffold(title: String, onBackClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBackClick) { Text("←") } },
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun NativeSettingsEntryItem(title: String, summary: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Text("进入") },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
    HorizontalDivider()
}