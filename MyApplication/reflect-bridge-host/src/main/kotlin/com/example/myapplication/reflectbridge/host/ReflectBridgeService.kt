package com.example.myapplication.reflectbridge.host

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolException
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.InternalAgentToolsApi
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool as ToolMark
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTool
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.asToolsByInterface
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.core.tools.serialization.serializeToolDescriptorsToJsonString
import com.example.myapplication.reflectbridge.BridgeFailureKind
import com.example.myapplication.reflectbridge.BridgeParameterKind
import com.example.myapplication.reflectbridge.BridgeToolParameterDto
import com.example.myapplication.reflectbridge.ReflectBridgeDiagnosticDto
import com.example.myapplication.reflectbridge.ReflectBridgeExecuteRequest
import com.example.myapplication.reflectbridge.ReflectBridgeExecuteResponse
import com.example.myapplication.reflectbridge.ReflectBridgeSnapshotDto
import com.example.myapplication.reflectbridge.ReflectBridgeToolDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.math.pow

private const val REFLECT_SOURCE = "reflect-bridge"

internal data class HostedReflectTool(
    val tool: Tool<*, *>,
    val registration: String,
    val implementationClass: String,
)

internal data class HostedReflectState(
    val tools: List<HostedReflectTool>,
    val diagnostics: List<ReflectBridgeDiagnosticDto>,
)

@ToolMark
@LLMDescription("Echoes a string using KFunction.asTool")
fun reflectEcho(@LLMDescription("Input text") text: String): String = "echo:$text"

@ToolMark
@LLMDescription("Raises base to exponent using ToolRegistry.Builder.tool(KFunction)")
fun raisePower(
    @LLMDescription("Base number") base: Int,
    @LLMDescription("Exponent") exponent: Int,
): Int = base.toDouble().pow(exponent.toDouble()).toInt()

@ToolMark
@LLMDescription("Uppercases text asynchronously using KFunction.asTool and a default parameter")
suspend fun delayedUppercase(
    @LLMDescription("Input text") text: String,
    @LLMDescription("How many copies to join") repeat: Int = 1,
): String = List(repeat.coerceAtLeast(1)) { text.uppercase() }.joinToString(separator = "|")

@Serializable
data class BridgePerson(val name: String, val age: Int)

data class NonSerializableBridgePayload(val value: String)

interface BridgeMathInterface : ToolSet {
    @ToolMark
    @LLMDescription("Adds two integers via asToolsByInterface")
    fun interfaceAdd(
        @LLMDescription("First number") a: Int,
        @LLMDescription("Second number") b: Int,
    ): Int
}

class BridgeMathImpl : BridgeMathInterface {
    override fun interfaceAdd(a: Int, b: Int): Int = a + b

    @ToolMark
    @LLMDescription("This tool exists on the class, but should be hidden when using asToolsByInterface")
    fun hiddenMultiply(
        @LLMDescription("First number") a: Int,
        @LLMDescription("Second number") b: Int,
    ): Int = a * b
}

@ToolMark
@Suppress("unused")
fun brokenNonSerializableArg(
    @LLMDescription("Payload that is intentionally not serializable") payload: NonSerializableBridgePayload,
): String = payload.value

@LLMDescription("Reflect bridge string and person tools")
class BridgeToolSet : ToolSet {
    @ToolMark
    @LLMDescription("Concatenates two strings")
    fun concat(@LLMDescription("Left value") a: String, @LLMDescription("Right value") b: String): String = a + b

    @ToolMark
    @LLMDescription("Creates a person object")
    fun createPerson(@LLMDescription("Name") name: String, @LLMDescription("Age") age: Int): BridgePerson = BridgePerson(name, age)

    @ToolMark
    @LLMDescription("Formats a person object")
    fun formatPerson(@LLMDescription("Person") person: BridgePerson): String = "${person.name} is ${person.age} years old"
}

object ReflectBridgeService {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(InternalAgentToolsApi::class)
    private val hostedState: HostedReflectState by lazy {
        val diagnostics = mutableListOf<ReflectBridgeDiagnosticDto>()
        val tools = buildList {
            registerTool(diagnostics, "KFunction.asTool / ToolFromCallable") {
                ::reflectEcho.asTool(name = "reflect_echo")
            }?.let { add(it) }
            registerTool(diagnostics, "KFunction.asTool / ToolFromCallable / suspend+default") {
                ::delayedUppercase.asTool(name = "delayed_uppercase")
            }?.let { add(it) }
            addAll(
                registerTools(diagnostics, "ToolSet.asTools") {
                    BridgeToolSet().asTools()
                }
            )
            addAll(
                registerTools(diagnostics, "ToolSet.asToolsByInterface") {
                    BridgeMathImpl().asToolsByInterface<BridgeMathInterface>()
                }
            )
            addAll(
                registerTools(diagnostics, "ToolRegistry.Builder.tool(KFunction)") {
                    ToolRegistry { tool(::raisePower) }.tools
                }
            )
            registerTool(diagnostics, "KFunction.asTool / registration failure demo") {
                ::brokenNonSerializableArg.asTool(name = "broken_non_serializable_arg")
            }
        }
        HostedReflectState(tools = tools, diagnostics = diagnostics)
    }

    fun snapshot(): ReflectBridgeSnapshotDto = ReflectBridgeSnapshotDto(
        hostName = "myapplication-reflect-bridge-host",
        tools = hostedState.tools.map { hosted ->
            ReflectBridgeToolDto(
                name = hosted.tool.name,
                description = hosted.tool.descriptor.description,
                source = REFLECT_SOURCE,
                registration = hosted.registration,
                implementationClass = hosted.implementationClass,
                schemaJson = serializeToolDescriptorsToJsonString(listOf(hosted.tool.descriptor)),
                parameters = hosted.tool.descriptor.requiredParameters.map { it.toDto(required = true) } +
                    hosted.tool.descriptor.optionalParameters.map { it.toDto(required = false) },
            )
        },
        diagnostics = hostedState.diagnostics,
    )

    @OptIn(InternalAgentToolsApi::class)
    fun execute(request: ReflectBridgeExecuteRequest): ReflectBridgeExecuteResponse {
        val hosted = hostedState.tools.firstOrNull { it.tool.name == request.toolName }
            ?: return ReflectBridgeExecuteResponse(
                toolName = request.toolName,
                source = REFLECT_SOURCE,
                registration = "unknown",
                status = "error",
                argsJson = request.argsJson,
                errorKind = BridgeFailureKind.UNKNOWN,
                error = "Tool not found: ${request.toolName}",
            )

        val rawArgs = runCatching { json.parseToJsonElement(request.argsJson).jsonObject }
            .getOrElse { error -> return hosted.failureResponse(request.argsJson, BridgeFailureKind.ARGUMENT_PARSE_FAILURE, error) }

        val decodedArgs = runCatching { hosted.tool.decodeArgs(rawArgs) }
            .getOrElse { error -> return hosted.failureResponse(request.argsJson, BridgeFailureKind.ARGUMENT_PARSE_FAILURE, error) }

        val result = runBlocking {
            runCatching { hosted.tool.executeUnsafe(decodedArgs) }
        }.getOrElse { error ->
            return hosted.failureResponse(request.argsJson, classifyExecutionFailure(error), error)
        }

        val resultText = runCatching { hosted.tool.encodeResultToStringUnsafe(result) }
            .getOrElse { error -> return hosted.failureResponse(request.argsJson, BridgeFailureKind.RESULT_SERIALIZATION_FAILURE, error) }

        return ReflectBridgeExecuteResponse(
            toolName = hosted.tool.name,
            source = REFLECT_SOURCE,
            registration = hosted.registration,
            status = "success",
            argsJson = request.argsJson,
            resultText = resultText,
        )
    }

    private fun HostedReflectTool.failureResponse(
        argsJson: String,
        failureKind: BridgeFailureKind,
        error: Throwable,
    ) = ReflectBridgeExecuteResponse(
        toolName = tool.name,
        source = REFLECT_SOURCE,
        registration = registration,
        status = "error",
        argsJson = argsJson,
        errorKind = failureKind,
        error = error.message ?: error::class.simpleName ?: "Unknown error",
    )

    private fun classifyExecutionFailure(error: Throwable): BridgeFailureKind = when (error) {
        is ToolException.ValidationFailure -> BridgeFailureKind.VALIDATION_FAILURE
        else -> BridgeFailureKind.EXECUTION_FAILURE
    }

    @OptIn(InternalAgentToolsApi::class)
    private fun registerTool(
        diagnostics: MutableList<ReflectBridgeDiagnosticDto>,
        registration: String,
        factory: () -> Tool<*, *>,
    ): HostedReflectTool? = runCatching { factory() }
        .fold(
            onSuccess = { HostedReflectTool(it, registration, it::class.simpleName ?: "Tool") },
            onFailure = { error ->
                diagnostics += ReflectBridgeDiagnosticDto(
                    registration = registration,
                    failureKind = BridgeFailureKind.REGISTRATION_FAILURE,
                    message = error.message ?: error::class.simpleName ?: "Unknown registration error",
                )
                null
            },
        )

    @OptIn(InternalAgentToolsApi::class)
    private fun registerTools(
        diagnostics: MutableList<ReflectBridgeDiagnosticDto>,
        registration: String,
        factory: () -> List<Tool<*, *>>,
    ): List<HostedReflectTool> = runCatching { factory() }
        .fold(
            onSuccess = { tools -> tools.map { HostedReflectTool(it, registration, it::class.simpleName ?: "Tool") } },
            onFailure = { error ->
                diagnostics += ReflectBridgeDiagnosticDto(
                    registration = registration,
                    failureKind = BridgeFailureKind.REGISTRATION_FAILURE,
                    message = error.message ?: error::class.simpleName ?: "Unknown registration error",
                )
                emptyList()
            },
        )

    private fun ai.koog.agents.core.tools.ToolParameterDescriptor.toDto(required: Boolean): BridgeToolParameterDto = BridgeToolParameterDto(
        name = name,
        description = description,
        kind = type.toBridgeKind(),
        typeLabel = type.toTypeLabel(),
        required = required,
        enumValues = (type as? ToolParameterType.Enum)?.entries?.toList().orEmpty(),
    )

    private fun ToolParameterType.toBridgeKind(): BridgeParameterKind = when (this) {
        ToolParameterType.String -> BridgeParameterKind.STRING
        ToolParameterType.Integer -> BridgeParameterKind.INTEGER
        ToolParameterType.Float -> BridgeParameterKind.FLOAT
        ToolParameterType.Boolean -> BridgeParameterKind.BOOLEAN
        ToolParameterType.Null -> BridgeParameterKind.NULL
        is ToolParameterType.Enum -> BridgeParameterKind.ENUM
        is ToolParameterType.List -> BridgeParameterKind.ARRAY
        is ToolParameterType.Object -> BridgeParameterKind.OBJECT
        is ToolParameterType.AnyOf -> BridgeParameterKind.JSON
    }

    private fun ToolParameterType.toTypeLabel(): String = when (this) {
        ToolParameterType.String -> "string"
        ToolParameterType.Integer -> "integer"
        ToolParameterType.Float -> "float"
        ToolParameterType.Boolean -> "boolean"
        ToolParameterType.Null -> "null"
        is ToolParameterType.Enum -> "enum(${entries.joinToString()})"
        is ToolParameterType.List -> "array<${itemsType.toTypeLabel()}>"
        is ToolParameterType.Object -> "object"
        is ToolParameterType.AnyOf -> "json"
    }
}