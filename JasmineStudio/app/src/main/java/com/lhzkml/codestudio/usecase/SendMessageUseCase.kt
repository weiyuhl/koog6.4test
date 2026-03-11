package com.lhzkml.codestudio.usecase

import com.lhzkml.codestudio.AgentRunner
import com.lhzkml.codestudio.State
import com.lhzkml.codestudio.toAgentRequest

internal class SendMessageUseCase {
    
    suspend fun execute(
        state: State,
        userPrompt: String,
        onTextDelta: (String) -> Unit
    ): Result<AgentResult> {
        return try {
            val result = AgentRunner.runAgentStreaming(
                request = state.toAgentRequest(userPrompt),
                onTextDelta = onTextDelta
            )
            Result.success(AgentResult(result.answer, result.events))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
    
    data class AgentResult(
        val answer: String,
        val events: List<String>
    )
}
