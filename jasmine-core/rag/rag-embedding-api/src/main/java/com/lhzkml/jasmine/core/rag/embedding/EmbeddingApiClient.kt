package com.lhzkml.jasmine.core.rag.embedding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Embedding API 客户端，用于获取模型列表等
 * 支持 OpenAI 兼容的 /v1/models 端点
 */
object EmbeddingApiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    /**
     * 从 OpenAI 兼容 API 获取模型列表
     * 支持 sub_type=embedding 筛选（如硅基流动等），仅返回 Embedding 模型
     * @return 模型 id 列表，失败返回空列表
     */
    suspend fun listModels(baseUrl: String, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || apiKey.isBlank()) return@withContext emptyList()
        try {
            val url = baseUrl.trimEnd('/') + "/v1/models?sub_type=embedding"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            val root = json.parseToJsonElement(responseBody).jsonObject
            val dataArray = root["data"]?.jsonArray ?: return@withContext emptyList()
            dataArray.mapNotNull { elem ->
                (elem as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
