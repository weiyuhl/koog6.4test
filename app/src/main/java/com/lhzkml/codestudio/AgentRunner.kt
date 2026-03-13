package com.lhzkml.codestudio

import com.lhzkml.codestudio.service.ChatService
import com.lhzkml.jasmine.core.prompt.model.ChatMessage as JasmineChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Agent 执行器 - 使用 jasmine-core 供应商客户端
 */
object AgentRunner {
    
    private val chatService = ChatService()
    
    /**
     * 运行 Agent（非流式）
     */
    suspend fun runAgent(request: Request): ExecutionResult {
        val events = mutableListOf<String>()
        var answer = ""
        
        try {
            val result = runAgentStreaming(
                request = request,
                onTextDelta = { delta -> answer += delta },
                onEvent = { event -> events.add(event) }
            )
            return result
        } catch (e: Exception) {
            events.add("错误: ${e.message}")
            return ExecutionResult(
                answer = answer.ifBlank { "执行失败: ${e.message}" },
                events = events,
                runtimeSnapshot = null
            )
        }
    }
    
    /**
     * 运行 Agent（流式）
     */
    suspend fun runAgentStreaming(
        request: Request,
        onTextDelta: (String) -> Unit,
        onEvent: (String) -> Unit
    ): ExecutionResult = withContext(Dispatchers.IO) {
        val events = mutableListOf<String>()
        
        try {
            // 记录请求信息（不包含敏感信息）
            onEvent("供应商: ${request.provider.displayName}")
            onEvent("模型: ${request.modelId}")
            onEvent("Base URL: ${request.baseUrl.ifBlank { "使用默认" }}")
            
            // 创建客户端
            onEvent("正在连接 ${request.provider.displayName}...")
            val client = chatService.createClient(
                provider = request.provider,
                apiKey = request.apiKey,
                baseUrl = request.baseUrl,
                extraConfig = request.extraConfig
            )
            
            // 准备消息
            val messages = listOf(
                JasmineChatMessage(
                    role = "user",
                    content = request.userPrompt
                )
            )
            
            // 发送请求
            onEvent("正在生成回复...")
            val result = chatService.chat(
                client = client,
                messages = messages,
                modelId = request.modelId,
                systemPrompt = request.systemPrompt.takeIf { it.isNotBlank() },
                temperature = request.temperature,
                maxTokens = 4096,
                onChunk = onTextDelta
            )
            
            // 记录使用情况
            result.usage?.let { usage ->
                events.add("Token 使用: ${usage.promptTokens} (输入) + ${usage.completionTokens} (输出) = ${usage.totalTokens} (总计)")
            }
            
            result.finishReason?.let { reason ->
                events.add("完成原因: $reason")
            }
            
            onEvent("回复完成")
            
            ExecutionResult(
                answer = result.content,
                events = events,
                runtimeSnapshot = createRuntimeSnapshot(request, result)
            )
        } catch (e: Exception) {
            // 详细的错误信息
            val errorMessage = when {
                e.message?.contains("401") == true || e.message?.contains("Unauthorized") == true -> 
                    "认证失败: API Key 无效或已过期"
                e.message?.contains("403") == true || e.message?.contains("Forbidden") == true -> 
                    "无权访问: 请检查 API Key 权限或账户余额"
                e.message?.contains("404") == true -> 
                    "资源不存在: 请检查 Base URL 和模型 ID 是否正确"
                e.message?.contains("429") == true -> 
                    "请求过于频繁: 请稍后再试"
                e.message?.contains("500") == true || e.message?.contains("502") == true || e.message?.contains("503") == true -> 
                    "服务器错误: ${request.provider.displayName} 服务暂时不可用"
                e.message?.contains("timeout") == true || e.message?.contains("timed out") == true -> 
                    "请求超时: 网络连接超时，请检查网络或稍后重试"
                e.message?.contains("Unable to resolve host") == true || e.message?.contains("UnknownHost") == true -> 
                    "网络错误: 无法连接到服务器，请检查网络连接"
                else -> "执行失败: ${e.message ?: e::class.simpleName}"
            }
            
            events.add(errorMessage)
            onEvent(errorMessage)
            
            // 添加调试信息
            if (e.message != null) {
                events.add("详细错误: ${e.message}")
            }
            
            ExecutionResult(
                answer = "",
                events = events,
                runtimeSnapshot = null
            )
        }
    }
    
    private fun createRuntimeSnapshot(
        request: Request,
        result: ChatService.ChatResult
    ): RuntimeSnapshot {
        return RuntimeSnapshot(
            runId = System.currentTimeMillis().toString(),
            agentId = "chat-agent",
            strategyName = "direct-chat",
            providerName = request.provider.displayName,
            modelId = request.modelId,
            presetTitle = request.runtimePreset.title,
            nodeNames = listOf("ChatNode"),
            subgraphNames = emptyList(),
            availableToolNames = emptyList(),
            toolSourceSummaries = emptyList(),
            toolNames = emptyList(),
            llmModels = listOf(request.modelId),
            historyCount = 1,
            historyPreview = listOf(
                HistoryEntry(
                    role = "user",
                    contentPreview = request.userPrompt.take(100)
                ),
                HistoryEntry(
                    role = "assistant",
                    contentPreview = result.content.take(100)
                )
            ),
            storageEntries = emptyList(),
            timeline = listOf(
                TimelineEntry(
                    category = "LLM",
                    name = "Chat",
                    detail = "Model: ${request.modelId}, Tokens: ${result.usage?.totalTokens ?: 0}",
                    executionPath = null
                )
            ),
            finalResultPreview = result.content.take(200)
        )
    }
}
