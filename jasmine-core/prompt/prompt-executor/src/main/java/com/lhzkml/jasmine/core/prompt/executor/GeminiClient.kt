package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.llm.ChatClient
import com.lhzkml.jasmine.core.prompt.llm.ChatClientException
import com.lhzkml.jasmine.core.prompt.llm.ErrorType
import com.lhzkml.jasmine.core.prompt.llm.LLMProvider
import com.lhzkml.jasmine.core.prompt.llm.RetryConfig
import com.lhzkml.jasmine.core.prompt.llm.StreamResult
import com.lhzkml.jasmine.core.prompt.llm.executeWithRetry
import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.ChatResult
import com.lhzkml.jasmine.core.prompt.model.GeminiContent
import com.lhzkml.jasmine.core.prompt.model.GeminiFunctionCall
import com.lhzkml.jasmine.core.prompt.model.GeminiFunctionDeclaration
import com.lhzkml.jasmine.core.prompt.model.GeminiFunctionResponse
import com.lhzkml.jasmine.core.prompt.model.GeminiGenerationConfig
import com.lhzkml.jasmine.core.prompt.model.GeminiPart
import com.lhzkml.jasmine.core.prompt.model.GeminiRequest
import com.lhzkml.jasmine.core.prompt.model.GeminiResponse
import com.lhzkml.jasmine.core.prompt.model.GeminiToolDef
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
 * Google Gemini 客户端
 * 使用 Gemini 原生 generateContent API，支持 Tool Calling
 */
open class GeminiClient(
    protected val apiKey: String,
    protected val baseUrl: String = DEFAULT_BASE_URL,
    protected val retryConfig: RetryConfig = RetryConfig.DEFAULT,
    httpClient: OkHttpClient? = null,
    protected val generatePath: String = DEFAULT_GENERATE_PATH,
    protected val streamPath: String = DEFAULT_STREAM_PATH
) : ChatClient {

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"
        const val DEFAULT_GENERATE_PATH = "/v1beta/models/{model}:generateContent"
        const val DEFAULT_STREAM_PATH = "/v1beta/models/{model}:streamGenerateContent"
    }

    override val provider: LLMProvider = LLMProvider.Gemini

    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    internal val httpClient: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .connectTimeout(retryConfig.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(retryConfig.socketTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(retryConfig.socketTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(retryConfig.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    // ========== 消息转换 ==========

    /**
     * 将通用 ChatMessage 转换为 Gemini 格式
     * 支持 functionCall（assistant 带 tool_calls）和 functionResponse（tool 结果）
     */
    private fun convertMessages(messages: List<ChatMessage>): Pair<GeminiContent?, List<GeminiContent>> {
        var systemInstruction: GeminiContent? = null
        val contents = mutableListOf<GeminiContent>()

        for (msg in messages) {
            val msgToolCalls = msg.toolCalls
            when {
                msg.role == "system" -> {
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = msg.content)))
                }
                // assistant 消息带 tool_calls → model 角色 + functionCall parts
                msg.role == "assistant" && !msgToolCalls.isNullOrEmpty() -> {
                    val parts = mutableListOf<GeminiPart>()
                    if (msg.content.isNotEmpty()) {
                        parts.add(GeminiPart(text = msg.content))
                    }
                    msgToolCalls.forEach { tc ->
                        val argsJson = try {
                            json.parseToJsonElement(tc.arguments) as? JsonObject ?: buildJsonObject {}
                        } catch (_: Exception) {
                            buildJsonObject {}
                        }
                        parts.add(GeminiPart(functionCall = GeminiFunctionCall(name = tc.name, args = argsJson)))
                    }
                    contents.add(GeminiContent(role = "model", parts = parts))
                }
                // tool 结果消息 → user 角色 + functionResponse part
                msg.role == "tool" -> {
                    val responseJson = try {
                        json.parseToJsonElement(msg.content) as? JsonObject
                            ?: buildJsonObject { put("result", msg.content) }
                    } catch (_: Exception) {
                        buildJsonObject { put("result", msg.content) }
                    }
                    contents.add(GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(functionResponse = GeminiFunctionResponse(
                            name = msg.toolName ?: "",
                            response = responseJson
                        )))
                    ))
                }
                msg.role == "user" -> contents.add(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = msg.content)))
                )
                msg.role == "assistant" -> contents.add(
                    GeminiContent(role = "model", parts = listOf(GeminiPart(text = msg.content)))
                )
            }
        }
        return systemInstruction to contents
    }

    /**
     * 将 ToolDescriptor 转换为 Gemini tools 格式
     */
    private fun convertTools(tools: List<ToolDescriptor>): List<GeminiToolDef>? {
        if (tools.isEmpty()) return null
        return listOf(GeminiToolDef(
            functionDeclarations = tools.map { tool ->
                GeminiFunctionDeclaration(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.toJsonSchema()
                )
            }
        ))
    }

    /**
     * 从 GeminiResponse 中提取 ToolCall 列表
     */
    private fun extractToolCalls(response: GeminiResponse): List<ToolCall> {
        val parts = response.candidates?.firstOrNull()?.content?.parts ?: return emptyList()
        return parts.mapNotNull { part ->
            part.functionCall?.let { fc ->
                ToolCall(
                    id = "gemini_${fc.name}_${System.nanoTime()}",
                    name = fc.name,
                    arguments = fc.args?.toString() ?: "{}"
                )
            }
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
            toolCalls = streamResult.toolCalls
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
    ): StreamResult {
        return executeWithRetry(retryConfig) {
            try {
                val (systemInstruction, contents) = convertMessages(messages)
                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    generationConfig = GeminiGenerationConfig(
                        temperature = samplingParams?.temperature,
                        topP = samplingParams?.topP,
                        topK = samplingParams?.topK,
                        maxOutputTokens = maxTokens
                    ),
                    tools = convertTools(tools)
                )

                val requestBody = json.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())
                
                val url = "${baseUrl}${streamPath.replace("{model}", model)}?key=${apiKey}&alt=sse"
                val httpRequest = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val fullContent = StringBuilder()
                var lastUsage: Usage? = null
                var lastFinishReason: String? = null
                val toolCalls = mutableListOf<ToolCall>()

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
                                                    val chunk = json.decodeFromString<GeminiResponse>(data)
                                                    chunk.usageMetadata?.let {
                                                        lastUsage = Usage(
                                                            promptTokens = it.promptTokenCount,
                                                            completionTokens = it.candidatesTokenCount,
                                                            totalTokens = it.totalTokenCount
                                                        )
                                                    }
                                                    val firstCandidate = chunk.candidates?.firstOrNull()
                                                    if (firstCandidate?.finishReason != null) {
                                                        lastFinishReason = firstCandidate.finishReason
                                                    }
                                                    firstCandidate?.content?.parts?.forEach { part ->
                                                        val text = part.text
                                                        if (!text.isNullOrEmpty()) {
                                                            fullContent.append(text)
                                                            kotlinx.coroutines.runBlocking { onChunk(text) }
                                                        }
                                                        part.functionCall?.let { fc ->
                                                            toolCalls.add(ToolCall(
                                                                id = "gemini_${fc.name}_${System.nanoTime()}",
                                                                name = fc.name,
                                                                arguments = fc.args?.toString() ?: "{}"
                                                            ))
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

                StreamResult(
                    content = fullContent.toString(),
                    usage = lastUsage,
                    finishReason = lastFinishReason,
                    toolCalls = toolCalls
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
                val url = "${baseUrl}/v1beta/models?key=${apiKey}&pageSize=1000"
                val httpRequest = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(httpRequest).execute()
                    
                    if (!response.isSuccessful) {
                        val body = response.body?.string()
                        throw ChatClientException.fromStatusCode(provider.name, response.code, body)
                    }
                    
                    val body = response.body?.string() ?: return@withContext emptyList()
                    val geminiResponse = json.decodeFromString<com.lhzkml.jasmine.core.prompt.model.GeminiModelListResponse>(body)
                    geminiResponse.models
                        .filter { it.supportedGenerationMethods.contains("generateContent") }
                        .map { model ->
                            val id = model.name.removePrefix("models/")
                            ModelInfo(
                                id = id,
                                displayName = model.displayName.ifEmpty { null },
                                contextLength = model.inputTokenLimit,
                                maxOutputTokens = model.outputTokenLimit,
                                supportsThinking = model.thinking,
                                temperature = model.temperature,
                                maxTemperature = model.maxTemperature,
                                topP = model.topP,
                                topK = model.topK,
                                description = model.description.ifEmpty { null }
                            )
                        }
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
