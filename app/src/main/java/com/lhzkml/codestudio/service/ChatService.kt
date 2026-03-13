package com.lhzkml.codestudio.service

import com.lhzkml.codestudio.Provider
import com.lhzkml.jasmine.core.prompt.executor.*
import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.RetryConfig
import com.lhzkml.jasmine.core.prompt.model.ChatMessage as JasmineChatMessage
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * 聊天服务 - 封装 jasmine-core 的供应商客户端
 */
class ChatService {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * 创建聊天客户端
     */
    fun createClient(
        provider: Provider,
        apiKey: String,
        baseUrl: String,
        extraConfig: String
    ): ChatClient {
        return when (provider) {
            Provider.OPENAI -> {
                val url = baseUrl.ifBlank { OpenAIClient.DEFAULT_BASE_URL }
                OpenAIClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
            }
            
            Provider.ANTHROPIC -> {
                val url = baseUrl.ifBlank { ClaudeClient.DEFAULT_BASE_URL }
                ClaudeClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
            }
            
            Provider.GOOGLE -> {
                val url = baseUrl.ifBlank { GeminiClient.DEFAULT_BASE_URL }
                GeminiClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
            }
            
            Provider.DEEPSEEK -> {
                val url = baseUrl.ifBlank { DeepSeekClient.DEFAULT_BASE_URL }
                DeepSeekClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
            }
            
            Provider.SILICONFLOW -> {
                val url = baseUrl.ifBlank { SiliconFlowClient.DEFAULT_BASE_URL }
                SiliconFlowClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
            }
            
            Provider.DASHSCOPE -> {
                val url = baseUrl.ifBlank { "https://dashscope-intl.aliyuncs.com" }
                // DashScope 使用 OpenAI 兼容接口
                object : OpenAICompatibleClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                ) {
                    override val provider = com.lhzkml.jasmine.core.prompt.llm.LLMProvider.OpenAI
                }
            }
            
            Provider.OPENROUTER -> {
                val url = baseUrl.ifBlank { "https://openrouter.ai/api" }
                object : OpenAICompatibleClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                ) {
                    override val provider = com.lhzkml.jasmine.core.prompt.llm.LLMProvider.OpenAI
                }
            }
            
            Provider.OLLAMA -> {
                val url = baseUrl.ifBlank { "http://10.0.2.2:11434" }
                object : OpenAICompatibleClient(
                    apiKey = "ollama", // Ollama 不需要真实的 API key
                    baseUrl = url,
                    httpClient = httpClient
                ) {
                    override val provider = com.lhzkml.jasmine.core.prompt.llm.LLMProvider.OpenAI
                }
            }
            
            Provider.MISTRAL -> {
                val url = baseUrl.ifBlank { "https://api.mistral.ai" }
                object : OpenAICompatibleClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                ) {
                    override val provider = com.lhzkml.jasmine.core.prompt.llm.LLMProvider.OpenAI
                }
            }
            
            Provider.AZURE_OPENAI -> {
                // Azure OpenAI 需要特殊处理
                // baseUrl 应该包含完整的部署 URL
                // extraConfig 包含 api-version
                val apiVersion = extraConfig.ifBlank { "2024-10-21" }
                val fullUrl = if (baseUrl.contains("?")) {
                    "$baseUrl&api-version=$apiVersion"
                } else {
                    "$baseUrl?api-version=$apiVersion"
                }
                object : OpenAICompatibleClient(
                    apiKey = apiKey,
                    baseUrl = fullUrl,
                    httpClient = httpClient,
                    chatPath = "/chat/completions"
                ) {
                    override val provider = com.lhzkml.jasmine.core.prompt.llm.LLMProvider.OpenAI
                }
            }
            
            Provider.BEDROCK -> {
                throw UnsupportedOperationException("AWS Bedrock is not supported on Android")
            }
        }
    }
    
    /**
     * 发送聊天消息（流式）
     */
    suspend fun chat(
        client: ChatClient,
        messages: List<JasmineChatMessage>,
        modelId: String,
        systemPrompt: String?,
        temperature: Double?,
        maxTokens: Int? = 4096,
        onChunk: suspend (String) -> Unit
    ): ChatResult {
        // 如果有 system prompt，添加到消息列表开头
        val allMessages = if (!systemPrompt.isNullOrBlank()) {
            listOf(JasmineChatMessage(role = "system", content = systemPrompt)) + messages
        } else {
            messages
        }
        
        val samplingParams = SamplingParams(
            temperature = temperature ?: 0.7,
            topP = null,
            topK = null
        )
        
        val result = client.chatStreamWithUsage(
            messages = allMessages,
            model = modelId,
            maxTokens = maxTokens,
            samplingParams = samplingParams,
            tools = emptyList(),
            toolChoice = null,
            onChunk = onChunk
        )
        
        return ChatResult(
            content = result.content,
            usage = result.usage?.let {
                TokenUsage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens
                )
            },
            finishReason = result.finishReason
        )
    }
    
    data class ChatResult(
        val content: String,
        val usage: TokenUsage?,
        val finishReason: String?
    )
    
    data class TokenUsage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )
}
