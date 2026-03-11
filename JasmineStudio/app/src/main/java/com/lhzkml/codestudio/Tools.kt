package com.lhzkml.codestudio

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.validate
import ai.koog.agents.core.tools.validateNotNull
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock

object CurrentTimeTool : Tool<CurrentTimeTool.Args, CurrentTimeTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "current_time",
    description = "返回当前系统时间，适合回答现在几点或当前时间这类问题。",
) {
    @Serializable
    class Args

    @Serializable
    data class Result(val currentTime: String)

    override suspend fun execute(args: Args): Result = Result(Clock.System.now().toString())
}

object ValidationProbeTool : Tool<ValidationProbeTool.Args, ValidationProbeTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "validation_probe",
    description = "用于演示 validate / validateNotNull 失败路径。",
) {
    @Serializable
    data class Args(
        @property:LLMDescription("票据 ID，不能为空")
        val ticketId: String? = null,
        @property:LLMDescription("重试次数，必须在 1..3 之间")
        val attempts: Int,
    )

    @Serializable
    data class Result(val accepted: Boolean, val summary: String)

    override suspend fun execute(args: Args): Result {
        val ticketId = validateNotNull(args.ticketId?.takeIf { it.isNotBlank() }) { "ticketId 不能为空" }
        validate(args.attempts in 1..3) { "attempts 必须在 1..3 之间" }
        return Result(accepted = true, summary = "ticket=$ticketId, attempts=${args.attempts}")
    }
}

object ExecutionFailureTool : Tool<ExecutionFailureTool.Args, String>(
    argsSerializer = Args.serializer(),
    resultSerializer = String.serializer(),
    name = "execution_failure_probe",
    description = "用于演示工具执行期异常。",
) {
    @Serializable
    data class Args(@property:LLMDescription("失败原因") val reason: String)

    override suspend fun execute(args: Args): String = throw IllegalStateException("模拟执行失败：${args.reason}")
}

private object BrokenStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BrokenString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = decoder.decodeString()

    override fun serialize(encoder: Encoder, value: String) {
        throw SerializationException("故意触发结果序列化失败")
    }
}

object ResultSerializationFailureTool : Tool<ResultSerializationFailureTool.Args, String>(
    argsSerializer = Args.serializer(),
    resultSerializer = BrokenStringSerializer,
    name = "result_serialization_probe",
    description = "用于演示结果序列化失败路径。",
) {
    @Serializable
    data class Args(@property:LLMDescription("要返回的文本") val text: String)

    override suspend fun execute(args: Args): String = "result:${args.text}"
}

sealed class DemoMathTool(
    name: String,
    description: String,
) : Tool<DemoMathTool.Args, DemoMathTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = name,
    description = description,
) {
    @Serializable
    data class Args(
        @property:LLMDescription("第一个数字")
        val a: Double,
        @property:LLMDescription("第二个数字")
        val b: Double,
    )

    @Serializable
    data class Result(val value: Double)

    object Add : DemoMathTool("add_numbers", "对两个数字求和。") {
        override suspend fun execute(args: Args): Result = Result(args.a + args.b)
    }

    object Multiply : DemoMathTool("multiply_numbers", "对两个数字相乘。") {
        override suspend fun execute(args: Args): Result = Result(args.a * args.b)
    }
}

fun demoToolsCatalog(): List<Tool<*, *>> = listOf(
    CurrentTimeTool,
    DemoMathTool.Add,
    DemoMathTool.Multiply,
    ValidationProbeTool,
    ExecutionFailureTool,
    ResultSerializationFailureTool,
)
