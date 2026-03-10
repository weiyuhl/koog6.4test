package com.example.myapplication

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

internal data class RuntimeToolAssembly(
    val toolRegistry: ToolRegistry,
    val availableToolNames: List<String>,
    val toolSourceSummaries: List<String>,
    val promptAddendum: String,
)

internal fun assembleLocalCodeToolsWorkbenchDefinitions(config: CodeToolsConfig): List<ToolWorkbenchDefinition> {
    return if (!config.enabled) emptyList() else defaultCodeToolsCapabilities().map { it.asWorkbenchDefinition(config) }
}

internal fun assembleLocalCodeToolsRuntimeTools(config: CodeToolsConfig): List<Tool<*, *>> {
    return if (!config.enabled) emptyList() else defaultCodeToolsCapabilities().map { it.asRuntimeTool(config) }
}

internal suspend fun assembleRuntimeToolAssembly(request: AgentRequest): RuntimeToolAssembly {
    val localTools = demoToolsCatalog()
    val config = request.featureConfig.toCodeToolsConfig()
    val codeTools = assembleLocalCodeToolsRuntimeTools(config)
    val runtimeTools = localTools + codeTools
    val sourceSummaries = buildList {
        add("common-local=${localTools.size}")
        add("code-tools-local=${codeTools.size}")
    }
    val promptNotes = mutableListOf<String>()
    if (codeTools.isNotEmpty()) {
        promptNotes += "Android 本地 Code Tools 已启用，可直接列目录、读文件、写文件、编辑文件和执行正则搜索；需要文件内容时优先调用这些工具。"
        promptNotes += "当前本地工作区根目录：${effectiveCodeToolsWorkspaceRoot(config)}；允许路径前缀：${effectiveCodeToolsAllowedPrefixes(config).joinToString()}。"
        promptNotes += "当前 Android App 仅提供本地文件操作，不提供 shell 或 reflect 能力。"
    }
    val availableToolNames = runtimeTools.map(Tool<*, *>::name)
    if (availableToolNames.isNotEmpty()) promptNotes += "当前可用工具：${availableToolNames.joinToString()}。"
    return RuntimeToolAssembly(
        toolRegistry = ToolRegistry { tools(runtimeTools) },
        availableToolNames = availableToolNames,
        toolSourceSummaries = sourceSummaries,
        promptAddendum = promptNotes.joinToString("\n"),
    )
}

private fun AgentFeatureConfig.toCodeToolsConfig(): CodeToolsConfig = CodeToolsConfig(
    enabled = codeToolsEnabled,
    workspaceRoot = codeToolsWorkspaceRoot,
    allowedPathPrefixes = codeToolsAllowedPathPrefixes,
)

private fun CodeToolsCapability.asWorkbenchDefinition(config: CodeToolsConfig): ToolWorkbenchDefinition {
    val descriptor = runtimeDescriptor()
    return ToolWorkbenchDefinition(
        name = descriptor.name,
        description = descriptor.description,
        sourceLabel = "code-tools-local",
        registrationLabel = "Android local file tools",
        parameters = descriptor.workbenchParameters(),
        schemaJson = ai.koog.agents.core.tools.serialization.serializeToolDescriptorsToJsonString(listOf(descriptor)),
        execute = { rawInputs -> executeLocalCodeTool(operation, descriptor.name, config, rawInputs) },
    )
}

private fun CodeToolsCapability.asRuntimeTool(config: CodeToolsConfig): Tool<*, *> = LocalJsonTool(runtimeDescriptor()) { args ->
    executeLocalCodeTool(operation, runtimeDescriptor().name, config, args.entries.associate { it.key to it.value.toString().trim('"') }).toRuntimeResultText()
}

private fun CodeToolsCapability.runtimeDescriptor(): ToolDescriptor = ToolDescriptor(
    name = displayName,
    description = description,
    requiredParameters = when (operation) {
        CodeToolsOperationKind.LIST_DIRECTORY -> listOf(ToolParameterDescriptor("absolutePath", "目录路径；允许相对工作区根目录。", ToolParameterType.String))
        CodeToolsOperationKind.READ_FILE -> listOf(ToolParameterDescriptor("path", "文件路径；允许相对工作区根目录。", ToolParameterType.String))
        CodeToolsOperationKind.WRITE_FILE -> listOf(
            ToolParameterDescriptor("path", "文件路径；允许相对工作区根目录。", ToolParameterType.String),
            ToolParameterDescriptor("content", "完整文本内容。", ToolParameterType.String),
        )
        CodeToolsOperationKind.EDIT_FILE -> listOf(
            ToolParameterDescriptor("path", "文件路径；允许相对工作区根目录。", ToolParameterType.String),
            ToolParameterDescriptor("original", "需要精确替换的原始片段。", ToolParameterType.String),
            ToolParameterDescriptor("replacement", "替换后的文本。", ToolParameterType.String),
        )
        CodeToolsOperationKind.REGEX_SEARCH -> listOf(
            ToolParameterDescriptor("path", "文件或目录路径；允许相对工作区根目录。", ToolParameterType.String),
            ToolParameterDescriptor("regex", "正则表达式。", ToolParameterType.String),
        )
    },
    optionalParameters = when (operation) {
        CodeToolsOperationKind.LIST_DIRECTORY -> listOf(
            ToolParameterDescriptor("depth", "最大遍历深度，默认 1。", ToolParameterType.Integer),
            ToolParameterDescriptor("filter", "可选通配过滤，如 *.kt。", ToolParameterType.String),
        )
        CodeToolsOperationKind.READ_FILE -> listOf(
            ToolParameterDescriptor("startLine", "起始行，默认 0。", ToolParameterType.Integer),
            ToolParameterDescriptor("endLine", "结束行，默认 -1 表示文件末尾。", ToolParameterType.Integer),
        )
        CodeToolsOperationKind.WRITE_FILE, CodeToolsOperationKind.EDIT_FILE -> emptyList()
        CodeToolsOperationKind.REGEX_SEARCH -> listOf(
            ToolParameterDescriptor("limit", "最大结果条数，默认 25。", ToolParameterType.Integer),
            ToolParameterDescriptor("skip", "跳过前 N 个命中。", ToolParameterType.Integer),
            ToolParameterDescriptor("caseSensitive", "是否区分大小写。", ToolParameterType.Boolean),
        )
    },
)

private suspend fun executeLocalCodeTool(
    operation: CodeToolsOperationKind,
    toolName: String,
    config: CodeToolsConfig,
    rawInputs: Map<String, String>,
): ToolWorkbenchExecutionRecord {
    val timestamp = Clock.System.now().toString()
    val descriptor = defaultCodeToolsCapabilities().first { it.operation == operation }.runtimeDescriptor()
    val argsJson = runCatching { buildToolArgsJson(descriptor.workbenchParameters(), rawInputs).toString() }.getOrElse {
        return localFailureRecord(toolName, rawInputs, ToolWorkbenchFailureKind.ARGUMENT_PARSE_FAILURE, it.message ?: "参数解析失败", timestamp)
    }
    return try {
        val resultText = when (operation) {
            CodeToolsOperationKind.LIST_DIRECTORY -> listLocalCodeToolsDirectory(rawInputs.requireValue("absolutePath"), config, rawInputs.intValue("depth") ?: 1, rawInputs.optionalValue("filter")).root.renderTreeText()
            CodeToolsOperationKind.READ_FILE -> readLocalCodeToolsFile(rawInputs.requireValue("path"), config, rawInputs.intValue("startLine") ?: 0, rawInputs.intValue("endLine") ?: -1).let {
                listOfNotNull(it.warningMessage, it.file.textContent).joinToString("\n")
            }
            CodeToolsOperationKind.WRITE_FILE -> writeLocalCodeToolsFile(rawInputs.requireValue("path"), config, rawInputs.requireValue("content")).let { "Wrote ${it.file.path}" }
            CodeToolsOperationKind.EDIT_FILE -> editLocalCodeToolsFile(rawInputs.requireValue("path"), config, rawInputs.requireValue("original"), rawInputs.requireValue("replacement")).let {
                if (it.applied) it.updatedContent.orEmpty() else "未应用修改：${it.reason.orEmpty()}"
            }
            CodeToolsOperationKind.REGEX_SEARCH -> regexSearchLocalCodeTools(rawInputs.requireValue("path"), config, rawInputs.requireValue("regex"), rawInputs.intValue("limit") ?: 25, rawInputs.intValue("skip") ?: 0, rawInputs.booleanValue("caseSensitive") ?: false).renderSearchText()
        }
        ToolWorkbenchExecutionRecord(
            id = timestamp,
            toolName = toolName,
            sourceLabel = "code-tools-local",
            registrationLabel = "Android local file tools",
            status = "success",
            argsJson = argsJson,
            resultText = resultText,
            timestamp = timestamp,
        )
    } catch (error: LocalCodeToolsException) {
        localFailureRecord(toolName, rawInputs, error.kind.toWorkbenchFailureKind(), error.message, timestamp, argsJson)
    } catch (error: IllegalArgumentException) {
        localFailureRecord(toolName, rawInputs, ToolWorkbenchFailureKind.VALIDATION_FAILURE, error.message ?: "参数无效", timestamp, argsJson)
    }
}

private fun localFailureRecord(
    toolName: String,
    rawInputs: Map<String, String>,
    failureKind: ToolWorkbenchFailureKind,
    errorText: String,
    timestamp: String,
    argsJson: String = rawInputs.toString(),
) = ToolWorkbenchExecutionRecord(
    id = timestamp,
    toolName = toolName,
    sourceLabel = "code-tools-local",
    registrationLabel = "Android local file tools",
    status = "error",
    argsJson = argsJson,
    resultText = "",
    failureKind = failureKind,
    errorText = errorText,
    timestamp = timestamp,
)

internal fun LocalCodeToolsFailureKind.toWorkbenchFailureKind(): ToolWorkbenchFailureKind = when (this) {
    LocalCodeToolsFailureKind.VALIDATION_FAILURE -> ToolWorkbenchFailureKind.VALIDATION_FAILURE
    LocalCodeToolsFailureKind.PATH_VALIDATION_FAILURE -> ToolWorkbenchFailureKind.PATH_VALIDATION_FAILURE
    LocalCodeToolsFailureKind.FILE_NOT_FOUND -> ToolWorkbenchFailureKind.FILE_NOT_FOUND
    LocalCodeToolsFailureKind.NON_TEXT_FILE -> ToolWorkbenchFailureKind.NON_TEXT_FILE
    LocalCodeToolsFailureKind.PATCH_APPLY_FAILURE -> ToolWorkbenchFailureKind.PATCH_APPLY_FAILURE
    LocalCodeToolsFailureKind.REGEX_SEARCH_FAILURE -> ToolWorkbenchFailureKind.REGEX_SEARCH_FAILURE
    LocalCodeToolsFailureKind.UNKNOWN -> ToolWorkbenchFailureKind.UNKNOWN
}

private fun ToolWorkbenchExecutionRecord.toRuntimeResultText(): String = if (status == "success") resultText else "ERROR [${failureKind?.name ?: "UNKNOWN"}]: ${errorText.orEmpty()}"

private fun Map<String, String>.requireValue(name: String): String = get(name)?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("参数 $name 为必填项")

private fun Map<String, String>.optionalValue(name: String): String? = get(name)?.takeIf { it.isNotBlank() }

private fun Map<String, String>.intValue(name: String): Int? = optionalValue(name)?.toIntOrNull() ?: optionalValue(name)?.let { throw IllegalArgumentException("参数 $name 必须是整数") }

private fun Map<String, String>.booleanValue(name: String): Boolean? = optionalValue(name)?.let {
    when (it.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("参数 $name 必须是 true/false")
    }
}

private class LocalJsonTool(
    descriptor: ToolDescriptor,
    private val executeBlock: suspend (JsonObject) -> String,
) : Tool<JsonObject, String>(JsonObject.serializer(), String.serializer(), descriptor) {
    override suspend fun execute(args: JsonObject): String = executeBlock(args)

    override fun encodeResultToString(result: String): String = result
}