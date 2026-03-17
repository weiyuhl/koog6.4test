package com.lhzkml.jasmine.core.assistant.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * 网页搜索工具（OkHttp 架构对齐版）
 * 采用与 jasmine-core 统一的 OkHttp 引擎，替代原有的 Ktor 实现。
 * 核心逻辑保持不变：基于 DuckDuckGo Lite 的本地抓取方案。
 */
class WebSearchTool : Tool() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val fullLinkRegex = Regex("""<a\s[^>]*class=['"]result-link['"][^>]*>""", RegexOption.DOT_MATCHES_ALL)
    private val linkRegex = Regex("""<a[^>]+class=['"]result-link['"][^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
    private val hrefRegex = Regex("""href=['"]([^'"]*?)['"]""")
    private val snippetRegex = Regex("""<td[^>]+class=['"]result-snippet['"][^>]*>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL)
    private val htmlTagRegex = Regex("<[^>]*>")
    private val uddgRegex = Regex("""uddg=([^&]+)""")

    override val descriptor = ToolDescriptor(
        name = "web_search",
        description = "Search the web for current information. Returns titles, URLs, and snippets. Before answering questions about recent events, news, current prices, weather, or anything time-sensitive, search first. Also use this when you're unsure about facts or the user asks you to look something up.",
        requiredParameters = listOf(
            ToolParameterDescriptor("query", "The search query", ToolParameterType.StringType)
        )
    )

    override suspend fun execute(arguments: String): String {
        val args = Json.parseToJsonElement(arguments).jsonObject
        val query = args["query"]?.jsonPrimitive?.content ?: return "Error: Query is required"

        return try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "https://lite.duckduckgo.com/lite/?q=$encodedQuery"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (compatible; Kai/1.0)")
                .build()
            
            val html = withContext(Dispatchers.IO) {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                response.body?.string() ?: ""
            }
            
            val results = parseResults(html)

            if (results.isEmpty()) {
                Json.encodeToString(mapOf("success" to true, "results" to emptyList<String>(), "message" to "No results found"))
            } else {
                Json.encodeToString(mapOf("success" to true, "results" to results))
            }
        } catch (e: Exception) {
            "Error: Search failed: ${e.message}"
        }
    }

    private fun parseResults(html: String): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        val linkTags = fullLinkRegex.findAll(html).toList()
        val links = linkRegex.findAll(html).toList()
        val snippets = snippetRegex.findAll(html).toList()

        for (i in links.indices) {
            if (results.size >= 5) break
            val linkTag = linkTags.getOrNull(i)?.value ?: continue
            val href = hrefRegex.find(linkTag)?.groupValues?.get(1) ?: continue
            val title = links[i].groupValues[1].stripHtml().trim()
            val snippet = snippets.getOrNull(i)?.groupValues?.get(1)?.stripHtml()?.trim() ?: ""

            val finalUrl = extractUrlFromRedirect(href)

            if (finalUrl.isNotBlank() && title.isNotBlank()) {
                results.add(
                    mapOf(
                        "title" to title,
                        "url" to finalUrl,
                        "snippet" to snippet
                    )
                )
            }
        }
        return results
    }

    private fun extractUrlFromRedirect(href: String): String {
        val uddgParam = uddgRegex.find(href)?.groupValues?.get(1)
        if (uddgParam != null) {
            return try { 
                URLDecoder.decode(uddgParam, StandardCharsets.UTF_8.toString()) 
            } catch (e: Exception) { 
                href 
            }
        }
        return if (href.startsWith("//")) "https:$href" else href
    }

    private fun String.stripHtml(): String = replace(htmlTagRegex, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}
