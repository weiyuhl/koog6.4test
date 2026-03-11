package com.lhzkml.codestudio

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.annotations.InternalAgentToolsApi
import ai.koog.agents.core.tools.serialization.ToolJson
import ai.koog.agents.core.tools.serialization.serializeToolDescriptorsToJsonString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Clock

enum class ToolWorkbenchValueKind {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    NULL,
    ENUM,
    ARRAY,
    OBJECT,
    JSON,
}

data class ToolWorkbenchParameter(
    val name: String,
    val description: String,
    val typeLabel: String,
    val kind: ToolWorkbenchValueKind,
    val isRequired: Boolean,
    val enumValues: List<String> = emptyList(),
)

data class ToolWorkbenchDefinition(
    val name: String,
    val description: String,
    val sourceLabel: String,
    val registrationLabel: String,
    val parameters: List<ToolWorkbenchParameter>,
    val schemaJson: String,
    val diagnostics: List<ToolWorkbenchDiagnostic> = emptyList(),
    val execute: suspend (Map<String, String>) -> ToolWorkbenchExecutionRecord,
)

enum class ToolWorkbenchFailureKind {
    ARGUMENT_PARSE_FAILURE,
    VALIDATION_FAILURE,
    EXECUTION_FAILURE,
    RESULT_SERIALIZATION_FAILURE,
    PATH_VALIDATION_FAILURE,
    FILE_NOT_FOUND,
    NON_TEXT_FILE,
    PATCH_APPLY_FAILURE,
    REGEX_SEARCH_FAILURE,
    SHELL_DENIED,
    SHELL_TIMEOUT,
    SHELL_EXECUTION_FAILURE,
    TRANSPORT_FAILURE,
    REGISTRATION_FAILURE,
    UNKNOWN,
}

data class ToolWorkbenchDiagnostic(
    val registration: String,
    val failureKind: ToolWorkbenchFailureKind,
    val message: String,
)

data class ToolWorkbenchExecutionRecord(
    val id: String,
    val toolName: String,
    val sourceLabel: String,
    val registrationLabel: String,
    val status: String,
    val argsJson: String,
    val resultText: String,
    val resultPayload: String? = null,
    val failureKind: ToolWorkbenchFailureKind? = null,
    val errorText: String? = null,
    val timestamp: String,
)

fun ToolDescriptor.workbenchParameters(): List<ToolWorkbenchParameter> =
    requiredParameters.map { it.toWorkbenchParameter(isRequired = true) } +
        optionalParameters.map { it.toWorkbenchParameter(isRequired = false) }

fun Tool<*, *>.asWorkbenchDefinition(): ToolWorkbenchDefinition = ToolWorkbenchDefinition(
    name = name,
    description = descriptor.description,
    sourceLabel = "common-local",
    registrationLabel = "direct Tool",
    parameters = descriptor.workbenchParameters(),
    schemaJson = serializeToolDescriptorsToJsonString(listOf(descriptor)),
    execute = { rawInputs -> executeToolFromWorkbench(this, rawInputs) },
)

fun ToolParameterType.toWorkbenchTypeLabel(): String = when (this) {
    ToolParameterType.String -> "string"
    ToolParameterType.Integer -> "integer"
    ToolParameterType.Float -> "float"
    ToolParameterType.Boolean -> "boolean"
    ToolParameterType.Null -> "null"
    is ToolParameterType.Enum -> "enum(${entries.joinToString()})"
    is ToolParameterType.List -> "array<${itemsType.toWorkbenchTypeLabel()}>"
    is ToolParameterType.AnyOf -> "anyOf(${types.joinToString { it.name }})"
    is ToolParameterType.Object -> "object"
}

fun ToolParameterType.toWorkbenchKind(): ToolWorkbenchValueKind = when (this) {
    ToolParameterType.String -> ToolWorkbenchValueKind.STRING
    ToolParameterType.Integer -> ToolWorkbenchValueKind.INTEGER
    ToolParameterType.Float -> ToolWorkbenchValueKind.FLOAT
    ToolParameterType.Boolean -> ToolWorkbenchValueKind.BOOLEAN
    ToolParameterType.Null -> ToolWorkbenchValueKind.NULL
    is ToolParameterType.Enum -> ToolWorkbenchValueKind.ENUM
    is ToolParameterType.List -> ToolWorkbenchValueKind.ARRAY
    is ToolParameterType.Object -> ToolWorkbenchValueKind.OBJECT
    is ToolParameterType.AnyOf -> ToolWorkbenchValueKind.JSON
}

fun ToolParameterType.toWorkbenchInputHint(): String = toWorkbenchKind().toWorkbenchInputHint((this as? ToolParameterType.Enum)?.entries?.toList().orEmpty())

fun ToolWorkbenchValueKind.toWorkbenchInputHint(enumValues: List<String> = emptyList()): String = when (this) {
    ToolWorkbenchValueKind.STRING -> "输入文本"
    ToolWorkbenchValueKind.INTEGER -> "例如 42"
    ToolWorkbenchValueKind.FLOAT -> "例如 3.14"
    ToolWorkbenchValueKind.BOOLEAN -> "true 或 false"
    ToolWorkbenchValueKind.NULL -> "固定为 null"
    ToolWorkbenchValueKind.ENUM -> enumValues.joinToString(separator = " / ")
    ToolWorkbenchValueKind.ARRAY -> "输入 JSON 数组，例如 [1,2,3]"
    ToolWorkbenchValueKind.OBJECT -> "输入 JSON 对象，例如 {\"key\":\"value\"}"
    ToolWorkbenchValueKind.JSON -> "输入 JSON 值"
}

fun ToolWorkbenchValueKind.toKeyboardType(): androidx.compose.ui.text.input.KeyboardType = when (this) {
    ToolWorkbenchValueKind.INTEGER, ToolWorkbenchValueKind.FLOAT -> androidx.compose.ui.text.input.KeyboardType.Number
    else -> androidx.compose.ui.text.input.KeyboardType.Text
}

fun ToolWorkbenchValueKind.isSingleLineInput(): Boolean = this != ToolWorkbenchValueKind.OBJECT && this != ToolWorkbenchValueKind.ARRAY && this != ToolWorkbenchValueKind.JSON

fun buildToolArgsJson(tool: Tool<*, *>, rawInputs: Map<String, String>): JsonObject {
    return buildToolArgsJson(tool.descriptor.workbenchParameters(), rawInputs)
}

fun buildToolArgsJson(fields: List<ToolWorkbenchParameter>, rawInputs: Map<String, String>): JsonObject {
    return buildJsonObject {
        fields.forEach { field ->
            val raw = rawInputs[field.name]?.trim().orEmpty()
            if (raw.isEmpty()) {
                if (field.isRequired) error("参数 ${field.name} 为必填项")
                return@forEach
            }
            put(field.name, parseWorkbenchValue(field.kind, raw, field.enumValues))
        }
    }
}

@OptIn(InternalAgentToolsApi::class)
suspend fun executeToolFromWorkbench(
    tool: Tool<*, *>,
    rawInputs: Map<String, String>,
): ToolWorkbenchExecutionRecord {
    val timestamp = Clock.System.now().toString()
    val argsJson = runCatching { buildToolArgsJson(tool, rawInputs) }
        .getOrElse { error ->
            return failureRecord(
                toolName = tool.name,
                sourceLabel = "common-local",
                registrationLabel = "direct Tool",
                argsJson = "{}",
                failureKind = ToolWorkbenchFailureKind.ARGUMENT_PARSE_FAILURE,
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

    val decodedArgs = runCatching { tool.decodeArgs(argsJson) }
        .getOrElse { error ->
            return failureRecord(
                toolName = tool.name,
                sourceLabel = "common-local",
                registrationLabel = "direct Tool",
                argsJson = argsJson.toString(),
                failureKind = ToolWorkbenchFailureKind.ARGUMENT_PARSE_FAILURE,
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

    val result = runCatching { tool.executeUnsafe(decodedArgs) }
        .getOrElse { error ->
            return failureRecord(
                toolName = tool.name,
                sourceLabel = "common-local",
                registrationLabel = "direct Tool",
                argsJson = argsJson.toString(),
                failureKind = error.toWorkbenchFailureKind(),
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

    val resultText = runCatching { tool.encodeResultToStringUnsafe(result) }
        .getOrElse { error ->
            return failureRecord(
                toolName = tool.name,
                sourceLabel = "common-local",
                registrationLabel = "direct Tool",
                argsJson = argsJson.toString(),
                failureKind = ToolWorkbenchFailureKind.RESULT_SERIALIZATION_FAILURE,
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

    return ToolWorkbenchExecutionRecord(
        id = timestamp,
        toolName = tool.name,
        sourceLabel = "common-local",
        registrationLabel = "direct Tool",
        status = "success",
        argsJson = argsJson.toString(),
        resultText = resultText,
        timestamp = timestamp,
    )
}

@OptIn(InternalAgentToolsApi::class)
private fun parseWorkbenchValue(kind: ToolWorkbenchValueKind, raw: String, enumValues: List<String>): JsonElement = when (kind) {
    ToolWorkbenchValueKind.STRING -> JsonPrimitive(raw)
    ToolWorkbenchValueKind.INTEGER -> JsonPrimitive(raw.toLongOrNull() ?: error("整数参数格式错误: $raw"))
    ToolWorkbenchValueKind.FLOAT -> JsonPrimitive(raw.toDoubleOrNull() ?: error("浮点参数格式错误: $raw"))
    ToolWorkbenchValueKind.BOOLEAN -> when (raw.lowercase()) {
        "true" -> JsonPrimitive(true)
        "false" -> JsonPrimitive(false)
        else -> error("布尔参数只能是 true 或 false")
    }
    ToolWorkbenchValueKind.NULL -> JsonNull
    ToolWorkbenchValueKind.ENUM -> {
        require(enumValues.any { it == raw }) { "枚举参数必须是：${enumValues.joinToString()}" }
        JsonPrimitive(raw)
    }
    ToolWorkbenchValueKind.ARRAY -> {
        val element = ToolJson.decodeFromString(JsonElement.serializer(), raw)
        require(element is JsonArray) { "数组参数需要 JSON 数组" }
        element
    }
    ToolWorkbenchValueKind.OBJECT -> {
        val element = ToolJson.decodeFromString(JsonElement.serializer(), raw)
        require(element is JsonObject) { "对象参数需要 JSON 对象" }
        element
    }
    ToolWorkbenchValueKind.JSON -> ToolJson.decodeFromString(JsonElement.serializer(), raw)
}

private fun ai.koog.agents.core.tools.ToolParameterDescriptor.toWorkbenchParameter(isRequired: Boolean): ToolWorkbenchParameter = ToolWorkbenchParameter(
    name = name,
    description = description,
    typeLabel = type.toWorkbenchTypeLabel(),
    kind = type.toWorkbenchKind(),
    isRequired = isRequired,
    enumValues = (type as? ToolParameterType.Enum)?.entries?.toList().orEmpty(),
)

private fun failureRecord(
    toolName: String,
    sourceLabel: String,
    registrationLabel: String,
    argsJson: String,
    failureKind: ToolWorkbenchFailureKind,
    errorText: String,
    timestamp: String,
) = ToolWorkbenchExecutionRecord(
    id = timestamp,
    toolName = toolName,
    sourceLabel = sourceLabel,
    registrationLabel = registrationLabel,
    status = "error",
    argsJson = argsJson,
    resultText = "",
    failureKind = failureKind,
    errorText = errorText,
    timestamp = timestamp,
)

private fun Throwable.toWorkbenchFailureKind(): ToolWorkbenchFailureKind = when (this) {
    is ToolException.ValidationFailure -> ToolWorkbenchFailureKind.VALIDATION_FAILURE
    else -> ToolWorkbenchFailureKind.EXECUTION_FAILURE
}
