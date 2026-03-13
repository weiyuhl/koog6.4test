package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GeminiClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: GeminiClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = GeminiClient(
            apiKey = "test-api-key",
            baseUrl = mockServer.url("/").toString().trimEnd('/'),
            httpClient = OkHttpClient()
        )
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `test generate content success`() = runTest {
        val mockStreamResponse = """
            data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}],"usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":0,"totalTokenCount":10}}
            
            data: {"candidates":[{"content":{"parts":[{"text":"!"}],"role":"model"},"finishReason":"STOP"}],"usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":20,"totalTokenCount":30}}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "Hello"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "gemini-pro",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals("Hello!", result.content)
        assertEquals(10, result.usage?.promptTokens)
        assertEquals(20, result.usage?.completionTokens)
        assertEquals("STOP", result.finishReason)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("gemini-pro:streamGenerateContent"))
        assertTrue(recordedRequest.path!!.contains("key=test-api-key"))
    }

    @Test
    fun `test generate content with tool calls`() = runTest {
        val mockStreamResponse = """
            data: {"candidates":[{"content":{"parts":[{"functionCall":{"name":"get_weather","args":{"location":"Tokyo"}}}],"role":"model"},"finishReason":"STOP"}],"usageMetadata":{"promptTokenCount":15,"candidatesTokenCount":10,"totalTokenCount":25}}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "What's the weather in Tokyo?"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "gemini-pro",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals(1, result.toolCalls?.size)
        assertEquals("get_weather", result.toolCalls?.get(0)?.name)
        assertTrue(result.toolCalls?.get(0)?.arguments?.contains("Tokyo") == true)
    }

    @Test
    fun `test list models`() = runTest {
        val mockResponse = """
            {
                "models": [
                    {
                        "name": "models/gemini-pro",
                        "displayName": "Gemini Pro",
                        "description": "Best model for text",
                        "inputTokenLimit": 30720,
                        "outputTokenLimit": 2048,
                        "supportedGenerationMethods": ["generateContent", "streamGenerateContent"],
                        "temperature": 0.9,
                        "maxTemperature": 2.0,
                        "topP": 1.0,
                        "topK": 40
                    }
                ]
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val models = client.listModels()

        assertNotNull(models)
        assertEquals(1, models.size)
        assertEquals("gemini-pro", models[0].id)
        assertEquals("Gemini Pro", models[0].displayName)
        assertEquals(30720, models[0].contextLength)
        assertEquals(2048, models[0].maxOutputTokens)
    }
}
