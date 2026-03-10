package com.example.myapplication

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeToolsScreen(codeToolsEnabled: Boolean, codeToolsWorkspaceRoot: String, codeToolsAllowedPathPrefixes: String, onMenuClick: () -> Unit, onOpenSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    val config = remember(codeToolsEnabled, codeToolsWorkspaceRoot, codeToolsAllowedPathPrefixes) { CodeToolsConfig(enabled = codeToolsEnabled, workspaceRoot = codeToolsWorkspaceRoot, allowedPathPrefixes = codeToolsAllowedPathPrefixes) }
    val history = remember { mutableStateListOf<NativeToolHistoryEntry>() }
    var errorText by remember { mutableStateOf<String?>(null) }
    var latestOutput by remember { mutableStateOf<String?>(null) }
    var listPath by rememberSaveable(codeToolsWorkspaceRoot) { mutableStateOf(codeToolsWorkspaceRoot) }
    var listDepth by rememberSaveable { mutableStateOf("2") }
    var readPath by rememberSaveable { mutableStateOf("") }
    var writePath by rememberSaveable { mutableStateOf("") }
    var writeContent by rememberSaveable { mutableStateOf("") }
    var editPath by rememberSaveable { mutableStateOf("") }
    var editOriginal by rememberSaveable { mutableStateOf("") }
    var editReplacement by rememberSaveable { mutableStateOf("") }
    var searchPath by rememberSaveable(codeToolsWorkspaceRoot) { mutableStateOf(codeToolsWorkspaceRoot) }
    var searchRegex by rememberSaveable { mutableStateOf("") }

    fun record(title: String, detail: String, isError: Boolean = false) {
        latestOutput = detail
        history.add(0, NativeToolHistoryEntry(title, detail, isError))
        while (history.size > 8) history.removeLast()
    }
    fun runTool(title: String, block: () -> String) {
        errorText = null
        scope.launch {
            try { record(title, block()) } catch (error: Throwable) {
                errorText = error.message ?: error::class.simpleName ?: "Unknown error"
                record(title, errorText.orEmpty(), true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("本地工具")
                        Text("Android 本地文件操作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Text("☰") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NativeSectionCard(title = "工具状态", subtitle = "当前工具只操作 Android App 本地文件") {
                NativeInfoRow(label = "开关", value = if (codeToolsEnabled) "已启用" else "关闭")
                NativeInfoRow(label = "Workspace root", value = effectiveCodeToolsWorkspaceRoot(config))
                NativeInfoRow(label = "Allowed prefixes", value = effectiveCodeToolsAllowedPrefixes(config).joinToString())
                if (!codeToolsEnabled) {
                    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("去设置页开启") }
                }
                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }

            NativeSectionCard(title = "目录浏览", subtitle = "浏览工作区内目录") {
                NativeTextField(label = "absolutePath", value = listPath, onValueChange = { listPath = it }, placeholder = codeToolsWorkspaceRoot)
                NativeTextField(label = "depth", value = listDepth, onValueChange = { listDepth = it }, placeholder = "2", keyboardType = KeyboardType.Number)
                Button(onClick = { runTool("浏览目录") { listLocalCodeToolsDirectory(listPath, config, listDepth.toIntOrNull() ?: 2).root.renderTreeText() } }, enabled = codeToolsEnabled, modifier = Modifier.fillMaxWidth()) { Text("浏览目录") }
            }

            NativeSectionCard(title = "读取文件", subtitle = "读取文本文件内容") {
                NativeTextField(label = "path", value = readPath, onValueChange = { readPath = it }, placeholder = codeToolsWorkspaceRoot)
                Button(onClick = { runTool("读取文件") { readLocalCodeToolsFile(readPath, config).file.textContent.orEmpty() } }, enabled = codeToolsEnabled, modifier = Modifier.fillMaxWidth()) { Text("读取文件") }
            }

            NativeSectionCard(title = "写入文件", subtitle = "直接写入文本文件") {
                NativeTextField(label = "path", value = writePath, onValueChange = { writePath = it }, placeholder = codeToolsWorkspaceRoot)
                NativeTextField(label = "content", value = writeContent, onValueChange = { writeContent = it }, placeholder = "输入要写入的内容", singleLine = false)
                Button(onClick = { runTool("写入文件") { "Wrote ${writeLocalCodeToolsFile(writePath, config, writeContent).file.path}" } }, enabled = codeToolsEnabled, modifier = Modifier.fillMaxWidth()) { Text("写入文件") }
            }

            NativeSectionCard(title = "文本替换", subtitle = "做一次精确文本替换") {
                NativeTextField(label = "path", value = editPath, onValueChange = { editPath = it }, placeholder = codeToolsWorkspaceRoot)
                NativeTextField(label = "original", value = editOriginal, onValueChange = { editOriginal = it }, placeholder = "原始文本", singleLine = false)
                NativeTextField(label = "replacement", value = editReplacement, onValueChange = { editReplacement = it }, placeholder = "替换文本", singleLine = false)
                Button(onClick = { runTool("文本替换") { editLocalCodeToolsFile(editPath, config, editOriginal, editReplacement).updatedContent.orEmpty() } }, enabled = codeToolsEnabled, modifier = Modifier.fillMaxWidth()) { Text("执行替换") }
            }

            NativeSectionCard(title = "Regex 搜索", subtitle = "在目录或文件内执行搜索") {
                NativeTextField(label = "path", value = searchPath, onValueChange = { searchPath = it }, placeholder = codeToolsWorkspaceRoot)
                NativeTextField(label = "regex", value = searchRegex, onValueChange = { searchRegex = it }, placeholder = "TODO|FIXME")
                Button(onClick = { runTool("Regex 搜索") { regexSearchLocalCodeTools(searchPath, config, searchRegex).renderSearchText() } }, enabled = codeToolsEnabled, modifier = Modifier.fillMaxWidth()) { Text("执行搜索") }
            }

            NativeSectionCard(title = "最近结果", subtitle = "仅展示当前 App 内最近执行的本地工具结果") {
                latestOutput?.let { SelectionTextCard(it) } ?: Text("暂无结果", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                history.forEach { item -> ToolHistoryRow(item) }
            }
        }
    }
}

@Composable
private fun SelectionTextCard(text: String) {
    NativeSectionCard(title = "输出内容", subtitle = "最近一次执行结果") {
        SelectionContainer {
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ToolHistoryRow(item: NativeToolHistoryEntry) {
    NativeSectionCard(title = item.title, subtitle = if (item.isError) "执行失败" else "执行完成") {
        Text(
            text = item.detail,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

