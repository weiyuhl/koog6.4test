package com.example.myapplication

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolWorkbenchSupportTest {
    @Test
    fun buildToolArgsJson_returns_empty_object_for_no_arg_tool() {
        assertTrue(buildToolArgsJson(CurrentTimeTool, emptyMap()).isEmpty())
    }

    @Test
    fun buildToolArgsJson_parses_numeric_inputs_for_math_tool() {
        val json = buildToolArgsJson(
            DemoMathTool.Add,
            mapOf("a" to "1.5", "b" to "2.5"),
        )

        assertEquals(JsonPrimitive(1.5), json["a"])
        assertEquals(JsonPrimitive(2.5), json["b"])
    }

    @Test(expected = IllegalStateException::class)
    fun buildToolArgsJson_throws_for_missing_required_parameter() {
        buildToolArgsJson(DemoMathTool.Add, mapOf("a" to "1.0"))
    }

    @Test
    fun typeLabel_formats_list_type() {
        assertEquals(
            "array<string>",
            ai.koog.agents.core.tools.ToolParameterType.List(ai.koog.agents.core.tools.ToolParameterType.String).toWorkbenchTypeLabel(),
        )
    }

    @Test
    fun validation_probe_reports_validation_failure() = runBlocking {
        val record = executeToolFromWorkbench(
            tool = ValidationProbeTool,
            rawInputs = mapOf(
                "ticketId" to "",
                "attempts" to "2",
            ),
        )

        assertEquals("error", record.status)
        assertEquals(ToolWorkbenchFailureKind.VALIDATION_FAILURE, record.failureKind)
        assertTrue(record.errorText?.contains("ticketId") == true)
    }

    @Test
    fun execution_probe_reports_execution_failure() = runBlocking {
        val record = executeToolFromWorkbench(
            tool = ExecutionFailureTool,
            rawInputs = mapOf("reason" to "boom"),
        )

        assertEquals("error", record.status)
        assertEquals(ToolWorkbenchFailureKind.EXECUTION_FAILURE, record.failureKind)
        assertTrue(record.errorText?.contains("boom") == true)
    }

    @Test
    fun result_serialization_probe_reports_serialization_failure() = runBlocking {
        val record = executeToolFromWorkbench(
            tool = ResultSerializationFailureTool,
            rawInputs = mapOf("text" to "hello"),
        )

        assertEquals("error", record.status)
        assertEquals(ToolWorkbenchFailureKind.RESULT_SERIALIZATION_FAILURE, record.failureKind)
        assertTrue(record.errorText?.contains("序列化") == true)
    }
}