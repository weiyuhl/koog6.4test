package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.ThinkingChatClient
import com.lhzkml.jasmine.core.prompt.llm.ChatClientException
import com.lhzkml.jasmine.core.prompt.llm.ErrorType
import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.RetryConfig
import com.lhzkml.jasmine.core.prompt.llm.StreamResult
import com.lhzkml.jasmine.core.prompt.llm.executeWithRetry
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.ChatResult
import com.lhzkml.jasmine.core.prompt.model.ClaudeContentBlock
import com.lhzkml.jasmine.core.prompt.model.ClaudeMessage
import com.lhzkml.jasmine.core.prompt.model.ClaudeMessageContent
import com.lhzkml.jasmine.core.prompt.model.ClaudeRequest
import com.lhzkml.jasmine.core.prompt.model.ClaudeResponse
import com.lhzkml.jasmine.core.prompt.model.ClaudeStreamEvent
import com.lhzkml.jasmine.core.prompt.model.ClaudeToolDef
import com.lhzkml.jasmine.core.prompt.model.ModelInfo
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import com.lhzkml.jasmine.core.prompt.model.ToolCall
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.Usage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Anthropic Claude 客户端
 * 使用 Claude 原生 Messages API，支持 Tool Calling
 */
open class ClaudeClient(
    protected val apiKey: String,
    protected val baseUrl: String = DEFAULT_BASE_URL,
    protected val retryConfig: RetryConfig = RetryConfig.DEFAULT,
    httpClient: OkHttpClient? = null
) : ThinkingChatClient {

    companion object {
        const val DEFAULT_BASE_URL = "https://api.anthropic.com"
        const val ANTHROPIC_VERSION = "2023-06-01"
    }

    override val provider: LLMProvider = LLMProvider.Claude

    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    internal val httpClient: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .connectTimeout(retryConfig.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(retryConfig.socketTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(retryConfig.socketTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(retryConfig.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    // ========== 消息转换 ==========

    /**
     * 将通用 ChatMessage 转换为 Claude 格式
     * Claude 的 system 消息不在 messages 中，而是作为顶层字段
     * tool_use 结果通过 content blocks 传递
     */
    private fun convertMessages(messages: List<ChatMessage>): Pair<String?, List<ClaudeMessage>> {
        var systemPrompt: String? = null
        val claudeMessages = mutableListOf<ClaudeMessage>()

        for (msg in messages) {
            val msgToolCalls = msg.toolCalls
            when {
                msg.role == "system" -> systemPrompt = msg.content

                // assistant 消息带 tool_calls → 转为 tool_use content blocks
                msg.role == "assistant" && !msgToolCalls.isNullOrEmpty() -> {
                    val blocks = mutableListOf<ClaudeContentBlock>()
                    if (msg.content.isNotEmpty()) {
                        blocks.add(ClaudeContentBlock(type = "text", text = msg.content))
                    }
                    msgToolCalls.forEach { tc ->
                        val inputJson = try {
                            json.parseToJsonElement(tc.arguments) as? JsonObject ?: buildJsonObject {}
                        } catch (_: Exception) {
                            buildJsonObject {}
                        }
                        blocks.add(ClaudeContentBlock(type = "tool_use", id = tc.id, name = tc.name, input = inputJson))
                    }
                    claudeMessages.add(ClaudeMessage(role = "assistant", content = ClaudeMessageContent.Blocks(blocks)))
                }

                // tool 结果消息 → 转为 tool_result content block
                msg.role == "tool" -> {
                    val block = ClaudeContentBlock(type = "tool_result", toolUseId = msg.toolCallId, content = msg.content)
                    // Claude 要求 tool_result 在 user 角色消息中
                    claudeMessages.add(ClaudeMessage(role = "user", content = ClaudeMessageContent.Blocks(listOf(block))))
                }

                // 普通 user/assistant 消息
                msg.role == "user" || msg.role == "assistant" -> {
                    claudeMessages.add(ClaudeMessage(role = msg.role, content = ClaudeMessageContent.Text(msg.content)))
                }
            }
        }
        return systemPrompt to claudeMessages
    }

    /**
     * 将 ToolDescriptor 转换为 Claude 工具定义格式
     */
    private fun convertTools(tools: List<ToolDescriptor>): List<ClaudeToolDef>? {
        if (tools.isEmpty()) return null
        return tools.map { tool ->
            ClaudeToolDef(
                name = tool.name,
                description = tool.description,
                inputSchema = tool.toJsonSchema()
            )
        }
    }

    // ========== ChatClient 实现 ==========

    override suspend fun chat(
        messages: List<ChatMessage>, model: String, maxTokens: Int?,
        samplingParams: SamplingParams?, tools: List<ToolDescriptor>,
        toolChoice: com.lhzkml.jasmine.core.prompt.model.ToolChoice?
    ): String {
        return chatWithUsage(messages, model, maxTokens, samplingParams, tools, toolChoice).content
    }

    override suspend fun chatWithUsage(
        messages: List<ChatMessage>, model: String, maxTokens: Int?,
        samplingParams: SamplingParams?, tools: List<ToolDescriptor>,
        toolChoice: com.lhzkml.jasmine.core.prompt.model.ToolChoice?
    ): ChatResult {
        val content = StringBuilder()
        val streamResult = chatStreamWithUsage(messages, model, maxTokens, samplingParams, tools, toolChoice) { chunk ->
            content.append(chunk)
        }
        return ChatResult(
            content = content.toString(),
            usage = streamResult.usage,
            finishReason = streamResult.finishReason,
            toolCalls = streamResult.toolCalls,
            thinking = streamResult.thinking
        )
    }

    override fun chatStream(
        messages: List<ChatMessage>, model: String, maxTokens: Int?,
        samplingParams: SamplingParams?, tools: List<ToolDescriptor>,
        toolChoice: com.lhzkml.jasmine.core.prompt.model.ToolChoice?
    ): Flow<String> = flow {
        chatStreamWithUsage(messages, model, maxTokens, samplingParams, tools, toolChoice) { chunk ->
            emit(chunk)
        }
    }

    override suspend fun chatStreamWithUsage(
        messages: List<ChatMessage>, model: String, maxTokens: Int?,
        samplingParams: SamplingParams?, tools: List<ToolDescriptor>,
        toolChoice: com.lhzkml.jasmine.core.prompt.model.ToolChoice?,
        onChunk: suspend (String) -> Unit
    ): StreamResult = chatStreamWithThinking(messages, model, maxTokens, samplingParams, tools, toolChoice, onChunk, {})

    override suspend fun chatStreamWithThinking(
        messages: List<ChatMessage>, model: String, maxTokens: Int?,
        samplingParams: SamplingParams?, tools: List<ToolDescriptor>,
        toolChoice: com.lhzkml.jasmine.core.prompt.model.ToolChoice?,
        onChunk: suspend (String) -> Unit,
        onThinking: suspend (String) -> Unit
    ): StreamResult {
        return executeWithRetry(retryConfig) {
            try {
                val (systemPrompt, claudeMessages) = convertMessages(messages)
                val request = ClaudeRequest(
                    model = model,
                    messages = claudeMessages,
                    maxTokens = maxTokens ?: 4096,
                    system = systemPrompt,
                    stream = true,
                    temperature = samplingParams?.temperature,
                    topP = samplingParams?.topP,
                    topK = samplingParams?.topK,
                    tools = convertTools(tools)
                )

                val requestBody = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("${baseUrl}/v1/messages")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .post(requestBody)
                    .build()

                val fullContent = StringBuilder()
                var inputTokens = 0
                var outputTokens = 0
                var stopReason: String? = null
                val toolCallAccumulator = mutableMapOf<Int, Triple<String, String, StringBuilder>>()
                var currentBlockIndex = -1
                var currentBlockType = ""
                val thinkingContent = StringBuilder()

                withContext(Dispatchers.IO) {
                    suspendCancellableCoroutine<Unit> { continuation ->
                        val call = httpClient.newCall(httpRequest)
                        
                        continuation.invokeOnCancellation {
                            call.cancel()
                        }
                        
                        call.enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                continuation.resumeWithException(e)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                try {
                                    if (!response.isSuccessful) {
                                        val body = response.body?.string()
                                        continuation.resumeWithException(
                                            ChatClientException.fromStatusCode(provider.name, response.code, body)
                                        )
                                        return
                                    }

                                    response.body?.charStream()?.buffered()?.use { reader ->
                                        reader.lineSequence().forEach { line ->
                                            if (line.startsWith("data: ")) {
                                                val data = line.substring(6).trim()
                                                if (data.isEmpty()) return@forEach
                                                
                                                try {
                                                    val event = json.decodeFromString<ClaudeStreamEvent>(data)
                                                    when (event.type) {
                                                        "message_start" -> {
                                                            event.message?.usage?.let { inputTokens = it.inputTokens }
                                                        }
                                                        "content_block_start" -> {
                                                            val idx = event.index ?: 0
                                                            currentBlockIndex = idx
                                                            val block = event.contentBlock
                                                            currentBlockType = block?.type ?: "text"
                                                            if (currentBlockType == "tool_use") {
                                                                toolCallAccumulator[idx] = Triple(
                                                                    block?.id ?: "",
                                                                    block?.name ?: "",
                                                                    StringBuilder()
                                                                )
                                                            }
                                                        }
                                                        "content_block_delta" -> {
                                                            val delta = event.delta
                                                            if (delta != null) {
                                                                when (delta.type) {
                                                                    "text_delta" -> {
                                                                        val text = delta.text
                                                                        if (!text.isNullOrEmpty()) {
                                                                            fullContent.append(text)
                                                                            kotlinx.coroutines.runBlocking { onChunk(text) }
                                                                        }
                                                                    }
                                                                    "thinking_delta" -> {
                                                                        val text = delta.thinking
                                                                        if (!text.isNullOrEmpty()) {
                                                                            thinkingContent.append(text)
                                                                            kotlinx.coroutines.runBlocking { onThinking(text) }
                                                                        }
                                                                    }
                                                                    "input_json_delta" -> {
                                                                        val partial = delta.partialJson
                                                                        if (!partial.isNullOrEmpty()) {
                                                                            toolCallAccumulator[currentBlockIndex]?.let { (_, _, args) ->
                                                                                args.append(partial)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        "message_delta" -> {
                                                            event.usage?.let { outputTokens = it.outputTokens }
                                                            event.delta?.stopReason?.let { stopReason = it }
                                                        }
                                                    }
                                                } catch (_: Exception) { }
                                            }
                                        }
                                    }
                                    
                                    continuation.resume(Unit)
                                } catch (e: Exception) {
                                    continuation.resumeWithException(e)
                                }
                            }
                        })
                    }
                }

                val usage = Usage(
                    promptTokens = inputTokens,
                    completionTokens = outputTokens,
                    totalTokens = inputTokens + outputTokens
                )

                val toolCalls = toolCallAccumulator.entries
                    .sortedBy { it.key }
                    .map { (_, triple) ->
                        ToolCall(id = triple.first, name = triple.second, arguments = triple.third.toString().ifEmpty { "{}" })
                    }

                StreamResult(
                    content = fullContent.toString(),
                    usage = usage,
                    finishReason = stopReason,
                    toolCalls = toolCalls,
                    thinking = thinkingContent.toString().ifEmpty { null }
                )
            } catch (e: ChatClientException) {
                throw e
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                throw ChatClientException(provider.name, "无法连接到服务器，请检查网络", ErrorType.NETWORK, cause = e)
            } catch (e: ConnectException) {
                throw ChatClientException(provider.name, "连接失败，请检查网络", ErrorType.NETWORK, cause = e)
            } catch (e: SocketTimeoutException) {
                throw ChatClientException(provider.name, "请求超时，请稍后重试", ErrorType.NETWORK, cause = e)
            } catch (e: Exception) {
                throw ChatClientException(provider.name, "流式请求失败: ${e.message}", ErrorType.UNKNOWN, cause = e)
            }
        }
    }

    override suspend fun listModels(): List<ModelInfo> {
        return executeWithRetry(retryConfig) {
            try {
                val httpRequest = Request.Builder()
                    .url("${baseUrl}/v1/models?limit=1000")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .get()
                    .build()

                withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(httpRequest).execute()
                    
                    if (!response.isSuccessful) {
                        val body = response.body?.string()
                        throw ChatClientException.fromStatusCode(provider.name, response.code, body)
                    }
                    
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val claudeResponse = json.decodeFromString<com.lhzkml.jasmine.core.prompt.model.ClaudeModelListResponse>(body)
                    claudeResponse.data.map { ModelInfo(
                        id = it.id,
                        displayName = it.displayName.ifEmpty { null }
                    ) }
                }
            } catch (e: ChatClientException) {
                throw e
            } catch (e: Exception) {
                throw ChatClientException(provider.name, "获取模型列表失败: ${e.message}", ErrorType.UNKNOWN, cause = e)
            }
        }
    }

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}
