package com.lhzkml.jasmine.core.agent.tools

import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * 网络搜索工具集
 * 直接参考 koog 的 WebSearchTools 实现
 * 使用 BrightData SERP API 进行 Google 搜索和网页抓取
 */
class WebSearchTool(
    private val brightDataKey: String,
    private val serpZone: String = "serp_api1",
    private val unlockerZone: String = "web_unlocker1"
) : AutoCloseable {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Serializable
    data class WebSearchResult(val organic: List<OrganicResult> = emptyList()) {
        @Serializable
        data class OrganicResult(
            val link: String = "", val title: String = "", val description: String = "",
            val rank: Int = 0, val globalRank: Int = 0
        )
    }

    @Serializable
    data class WebPageScrapingResult(val body: String = "")

    @Serializable
    data class BrightDataRequest(val zone: String, val url: String, val format: String, val dataFormat: String? = null)

    private val httpClient = OkHttpClient.Builder().build()

    val search = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "web_search",
            description = "Search the web for real-time information about any topic. Returns summarized information from search results and relevant URLs. " +
                "Use this when you need up-to-date information that might not be available in your training data, or when you need to verify current facts. " +
                "This includes queries about libraries, frameworks, current events, or any informational queries. " +
                "Be specific and include relevant keywords for better results.",
            requiredParameters = listOf(
                ToolParameterDescriptor("query", "The search query. Be specific and include relevant keywords for better results. For technical queries, include version numbers or dates if relevant", ToolParameterType.StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("explanation", "One sentence explanation of why this search is being performed and how it contributes to the goal", ToolParameterType.StringType)
            )
        )
        override suspend fun execute(arguments: String): String {
            val obj = Json.parseToJsonElement(arguments).jsonObject
            val query = obj["query"]?.jsonPrimitive?.content ?: return "Error: Missing parameter 'query'"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.google.com/search?brd_json=1&q=$encodedQuery"
            val request = BrightDataRequest(zone = serpZone, url = searchUrl, format = "raw")
            return try {
                val requestBody = jsonParser.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("https://api.brightdata.com/request")
                    .addHeader("Authorization", "Bearer $brightDataKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()
                
                withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(httpRequest).execute()
                    if (!response.isSuccessful) return@withContext "Error: HTTP ${response.code}"
                    
                    val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                    val result = jsonParser.decodeFromString<WebSearchResult>(responseBody)
                    result.organic.joinToString("\n\n") { "[${it.rank}] ${it.title}\n${it.link}\n${it.description}" }
                        .ifEmpty { "No results found." }
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    val scrape = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "web_scrape",
            description = "Scrape a web page and return its content as markdown text. " +
                "Use this for reading web pages that may have anti-scraping protection (uses BrightData proxy). " +
                "The URL must be a fully-formed, valid URL. This tool is read-only.",
            requiredParameters = listOf(
                ToolParameterDescriptor("url", "The fully-formed URL to scrape", ToolParameterType.StringType)
            )
        )
        override suspend fun execute(arguments: String): String {
            val obj = Json.parseToJsonElement(arguments).jsonObject
            val url = obj["url"]?.jsonPrimitive?.content ?: return "Error: Missing parameter 'url'"
            val request = BrightDataRequest(zone = unlockerZone, url = url, format = "json", dataFormat = "markdown")
            return try {
                val requestBody = jsonParser.encodeToString(request)
                    .toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("https://api.brightdata.com/request")
                    .addHeader("Authorization", "Bearer $brightDataKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()
                
                withContext(Dispatchers.IO) {
                    val response = httpClient.newCall(httpRequest).execute()
                    if (!response.isSuccessful) return@withContext "Error: HTTP ${response.code}"
                    
                    val responseBody = response.body?.string() ?: return@withContext "Error: Empty response"
                    jsonParser.decodeFromString<WebPageScrapingResult>(responseBody).body.ifEmpty { "No content found." }
                }
            } catch (e: Exception) { "Error: ${e.message}" }
        }
    }

    fun allTools(): List<Tool> = listOf(search, scrape)

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}
