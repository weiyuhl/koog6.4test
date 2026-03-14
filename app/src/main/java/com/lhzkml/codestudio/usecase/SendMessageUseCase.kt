package com.lhzkml.codestudio.usecase

import com.lhzkml.codestudio.*
import com.lhzkml.codestudio.repository.ChatRepository
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.validation.ValidationService
import javax.inject.Inject

internal class SendMessageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository
) {
    private val agentRunner: AgentRunner = AgentRunner

    
    suspend fun execute(
        request: SendMessageRequest,
        onTextDelta: (String) -> Unit
    ): Result<SendMessageResult> {
        return try {
            // 使用统一的校验服务
            val validation = ValidationService.validateSendMessageRequest(
                userPrompt = request.userPrompt,
                provider = request.provider,
                apiKey = request.apiKey,
                modelId = request.modelId,
                baseUrl = request.baseUrl
            )
            if (validation != null) {
                return Result.failure(ValidationException(validation))
            }
            
            // 执行 Agent
            val result = agentRunner.runAgentStreaming(
                request = request.toAgentRequest(),
                onTextDelta = onTextDelta,
                onEvent = { }
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
        requiresBaseUrl = false // 所有供应商都使用可选的 Base URL
    )
}
