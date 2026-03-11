package com.lhzkml.codestudio.usecase

import com.lhzkml.codestudio.*

internal class SendMessageUseCase(
    private val agentRunner: AgentRunner = AgentRunner
) {
    
    suspend fun execute(
        request: SendMessageRequest,
        onTextDelta: (String) -> Unit
    ): Result<SendMessageResult> {
        return try {
            // 验证请求
            val validation = validateRequest(request)
            if (validation != null) {
                return Result.failure(ValidationException(validation))
            }
            
            // 执行 Agent
            val result = agentRunner.runAgentStreaming(
                request = request.toAgentRequest(),
                onTextDelta = onTextDelta
            )
            
            // 处理结果
            Result.success(
                SendMessageResult(
                    answer = result.answer.takeIf { it.isNotBlank() } 
                        ?: "未收到回复",
                    events = result.events,
                    hasEvents = result.events.isNotEmpty()
                )
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
    
    private fun validateRequest(request: SendMessageRequest): String? {
        return when {
            request.userPrompt.isBlank() -> "消息不能为空"
            request.apiKey.isBlank() && request.requiresApiKey -> "API Key 不能为空"
            request.modelId.isBlank() -> "模型 ID 不能为空"
            request.baseUrl.isBlank() && request.requiresBaseUrl -> "Base URL 不能为空"
            else -> null
        }
    }
    
    private fun SendMessageRequest.toAgentRequest(): Request {
        return Request(
            provider = provider,
            apiKey = apiKey.trim(),
            modelId = modelId.trim(),
            baseUrl = baseUrl.trim(),
            extraConfig = extraConfig.trim(),
            systemPrompt = systemPrompt.trim(),
            temperature = temperature.toDoubleOrNull() ?: 0.2,
            maxIterations = maxIterations.toIntOrNull() ?: 50,
            userPrompt = userPrompt.trim(),
            runtimePreset = runtimePreset
        )
    }
    
    data class SendMessageRequest(
        val provider: Provider,
        val apiKey: String,
        val modelId: String,
        val baseUrl: String,
        val extraConfig: String,
        val systemPrompt: String,
        val temperature: String,
        val maxIterations: String,
        val userPrompt: String,
        val runtimePreset: Preset,
        val requiresApiKey: Boolean,
        val requiresBaseUrl: Boolean
    )
    
    data class SendMessageResult(
        val answer: String,
        val events: List<String>,
        val hasEvents: Boolean
    )
    
    class ValidationException(message: String) : Exception(message)
}

internal fun State.toSendMessageRequest(userPrompt: String): SendMessageUseCase.SendMessageRequest {
    return SendMessageUseCase.SendMessageRequest(
        provider = provider,
        apiKey = apiKey,
        modelId = modelId,
        baseUrl = baseUrl,
        extraConfig = extraConfig,
        systemPrompt = systemPrompt,
        temperature = temperature,
        maxIterations = maxIterations,
        userPrompt = userPrompt,
        runtimePreset = runtimePreset,
        requiresApiKey = provider.requiresApiKey,
        requiresBaseUrl = provider == Provider.AZURE_OPENAI || provider == Provider.OLLAMA
    )
}
