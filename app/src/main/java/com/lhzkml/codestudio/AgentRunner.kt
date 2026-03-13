package com.lhzkml.codestudio

// AgentRunner functionality removed - koog framework integration disabled

object AgentRunner {
    suspend fun runAgent(request: Request): ExecutionResult {
        return ExecutionResult(
            answer = "Koog framework has been disabled. Agent execution is not available.",
            events = listOf("Koog framework integration removed"),
            runtimeSnapshot = null
        )
    }

    suspend fun runAgentStreaming(
        request: Request,
        onTextDelta: (String) -> Unit,
        onEvent: (String) -> Unit
    ): ExecutionResult {
        onEvent("Koog framework integration removed")
        return runAgent(request)
    }
}
