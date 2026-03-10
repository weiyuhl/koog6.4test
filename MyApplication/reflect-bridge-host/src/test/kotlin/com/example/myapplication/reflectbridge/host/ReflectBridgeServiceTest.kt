package com.example.myapplication.reflectbridge.host

import com.example.myapplication.reflectbridge.ReflectBridgeExecuteRequest
import com.example.myapplication.reflectbridge.BridgeFailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectBridgeServiceTest {
    @Test
    fun snapshot_exposes_expected_reflect_registration_paths() {
        val snapshot = ReflectBridgeService.snapshot()

        assertTrue(snapshot.tools.any { it.registration.contains("asTool") })
        assertTrue(snapshot.tools.any { it.registration.contains("asTools") })
        assertTrue(snapshot.tools.any { it.registration.contains("asToolsByInterface") })
        assertTrue(snapshot.tools.any { it.registration.contains("Builder.tool") })
        assertTrue(snapshot.diagnostics.any { it.failureKind == BridgeFailureKind.REGISTRATION_FAILURE })
    }

    @Test
    fun execute_runs_reflect_tool_successfully() {
        val response = ReflectBridgeService.execute(
            ReflectBridgeExecuteRequest(
                toolName = "reflect_echo",
                argsJson = "{\"text\":\"hello\"}",
            ),
        )

        assertEquals("success", response.status)
        assertEquals("\"echo:hello\"", response.resultText)
    }

    @Test
    fun execute_reports_argument_parse_and_validation_failures() {
        val parseFailure = ReflectBridgeService.execute(
            ReflectBridgeExecuteRequest(
                toolName = "interfaceAdd",
                argsJson = "{\"a\":\"bad\",\"b\":2}",
            ),
        )
        val validationLike = ReflectBridgeService.execute(
            ReflectBridgeExecuteRequest(
                toolName = "delayed_uppercase",
                argsJson = "{\"text\":\"ok\",\"repeat\":2}",
            ),
        )

        assertEquals(BridgeFailureKind.ARGUMENT_PARSE_FAILURE, parseFailure.errorKind)
        assertEquals("success", validationLike.status)
        assertEquals("\"OK|OK\"", validationLike.resultText)
    }
}