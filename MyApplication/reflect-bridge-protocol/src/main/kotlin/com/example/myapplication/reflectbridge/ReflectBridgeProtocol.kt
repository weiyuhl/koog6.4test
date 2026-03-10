package com.example.myapplication.reflectbridge

import kotlinx.serialization.Serializable

@Serializable
enum class BridgeParameterKind {
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

@Serializable
enum class BridgeFailureKind {
    ARGUMENT_PARSE_FAILURE,
    VALIDATION_FAILURE,
    EXECUTION_FAILURE,
    RESULT_SERIALIZATION_FAILURE,
    REGISTRATION_FAILURE,
    TRANSPORT_FAILURE,
    UNKNOWN,
}

@Serializable
data class BridgeToolParameterDto(
    val name: String,
    val description: String,
    val kind: BridgeParameterKind,
    val typeLabel: String,
    val required: Boolean,
    val enumValues: List<String> = emptyList(),
)

@Serializable
data class ReflectBridgeToolDto(
    val name: String,
    val description: String,
    val source: String,
    val registration: String,
    val implementationClass: String,
    val schemaJson: String,
    val parameters: List<BridgeToolParameterDto>,
)

@Serializable
data class ReflectBridgeDiagnosticDto(
    val registration: String,
    val failureKind: BridgeFailureKind,
    val message: String,
)

@Serializable
data class ReflectBridgeSnapshotDto(
    val hostName: String,
    val tools: List<ReflectBridgeToolDto>,
    val diagnostics: List<ReflectBridgeDiagnosticDto> = emptyList(),
)

@Serializable
data class ReflectBridgeExecuteRequest(
    val toolName: String,
    val argsJson: String,
)

@Serializable
data class ReflectBridgeExecuteResponse(
    val toolName: String,
    val source: String,
    val registration: String,
    val status: String,
    val argsJson: String,
    val resultText: String? = null,
    val errorKind: BridgeFailureKind? = null,
    val error: String? = null,
)