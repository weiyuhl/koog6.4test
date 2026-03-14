package com.lhzkml.codestudio.service

import android.content.Context
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
    
    private var appContext: Context? = null
    
    fun setContext(context: Context) {
        appContext = context
    }
    
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
            
            Provider.OPENROUTER -> {
                val url = baseUrl.ifBlank { OpenRouterClient.DEFAULT_BASE_URL }
                OpenRouterClient(
                    apiKey = apiKey,
                    baseUrl = url,
                    httpClient = httpClient
                )
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
    
    /**
     * 查询账户余额
     */
    suspend fun getBalance(
        client: ChatClient
    ): BalanceResult {
        return try {
            val balance = client.getBalance()
            if (balance != null) {
                BalanceResult.Success(balance.toDisplayString())
            } else {
                BalanceResult.NotSupported
            }
        } catch (e: Exception) {
            BalanceResult.Error(e.message ?: "查询失败")
        }
    }
    
    sealed class BalanceResult {
        data class Success(val balance: String) : BalanceResult()
        data class Error(val message: String) : BalanceResult()
        object NotSupported : BalanceResult()
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
    
    // ========== OpenRouter 特有功能 ==========
    
    /**
     * 获取指定模型的端点信息（仅 OpenRouter）
     */
    suspend fun getModelEndpoints(
        client: ChatClient,
        author: String,
        slug: String
    ): OpenRouterEndpointsResult {
        return try {
            if (client is OpenRouterClient) {
                val response = client.getModelEndpoints(author, slug)
                OpenRouterEndpointsResult.Success(response)
            } else {
                OpenRouterEndpointsResult.NotSupported
            }
        } catch (e: Exception) {
            OpenRouterEndpointsResult.Error(e.message ?: "获取端点信息失败")
        }
    }
    
    /**
     * 获取生成请求的详细统计信息（仅 OpenRouter）
     */
    suspend fun getGenerationStats(
        client: ChatClient,
        generationId: String
    ): OpenRouterGenerationResult {
        return try {
            if (client is OpenRouterClient) {
                val response = client.getGeneration(generationId)
                OpenRouterGenerationResult.Success(response)
            } else {
                OpenRouterGenerationResult.NotSupported
            }
        } catch (e: Exception) {
            OpenRouterGenerationResult.Error(e.message ?: "获取生成统计失败")
        }
    }
    
    /**
     * 获取 API 密钥信息（仅 OpenRouter）
     */
    suspend fun getKeyInfo(
        client: ChatClient
    ): OpenRouterKeyInfoResult {
        return try {
            if (client is OpenRouterClient) {
                val response = client.getKeyInfo()
                OpenRouterKeyInfoResult.Success(response)
            } else {
                OpenRouterKeyInfoResult.NotSupported
            }
        } catch (e: Exception) {
            OpenRouterKeyInfoResult.Error(e.message ?: "获取密钥信息失败")
        }
    }
    
    sealed class OpenRouterEndpointsResult {
        data class Success(val data: com.lhzkml.jasmine.core.prompt.executor.ModelEndpointsResponse) : OpenRouterEndpointsResult()
        data class Error(val message: String) : OpenRouterEndpointsResult()
        object NotSupported : OpenRouterEndpointsResult()
    }
    
    sealed class OpenRouterGenerationResult {
        data class Success(val data: com.lhzkml.jasmine.core.prompt.executor.GenerationResponse) : OpenRouterGenerationResult()
        data class Error(val message: String) : OpenRouterGenerationResult()
        object NotSupported : OpenRouterGenerationResult()
    }
    
    sealed class OpenRouterKeyInfoResult {
        data class Success(val data: com.lhzkml.jasmine.core.prompt.executor.KeyInfoResponse) : OpenRouterKeyInfoResult()
        data class Error(val message: String) : OpenRouterKeyInfoResult()
        object NotSupported : OpenRouterKeyInfoResult()
    }
}
