package com.lhzkml.jasmine.core.agent.tools

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WebSearchToolTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var tool: WebSearchTool
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        tool = WebSearchTool(
            apiKey = "test-api-key",
            baseUrl = mockServer.url("/").toString().trimEnd('/'),
            httpClient = OkHttpClient(),
            json = json
        )
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `test web search success`() = runTest {
        val mockResponse = """
            {
                "results": [
                    {
                        "title": "Test Result 1",
                        "url": "https://example.com/1",
                        "snippet": "This is a test result"
                    },
                    {
                        "title": "Test Result 2",
                        "url": "https://example.com/2",
                        "snippet": "Another test result"
                    }
                ]
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val result = tool.search("test query")

        assertNotNull(result)
        assertTrue(result.contains("Test Result 1"))
        assertTrue(result.contains("https://example.com/1"))
        assertTrue(result.contains("This is a test result"))
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("test+query") || 
                   recordedRequest.path!!.contains("test%20query"))
    }

    @Test
    fun `test web search with empty results`() = runTest {
        val mockResponse = """
            {
                "results": []
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val result = tool.search("nonexistent query")

        assertNotNull(result)
        assertTrue(result.contains("No results found") || result.isEmpty())
    }

    @Test
    fun `test web search error handling`() = runTest {
        mockServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))

        try {
            tool.search("error query")
            fail("Expected exception for server error")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("500") == true || 
                      e.message?.contains("error") == true)
        }
    }
}
