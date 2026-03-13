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
            val errorMessage = "执行失败: ${e.message ?: e::class.simpleName}"
            events.add(errorMessage)
            onEvent(errorMessage)
            
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
