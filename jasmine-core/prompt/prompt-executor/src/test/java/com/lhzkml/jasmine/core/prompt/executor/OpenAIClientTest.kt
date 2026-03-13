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

class OpenAIClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: OpenAIClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = OpenAIClient(
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
    fun `test chat completion success`() = runTest {
        val mockStreamResponse = """
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"gpt-4","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "Hello"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "gpt-4",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals("Hello!", result.content)
        assertEquals(10, result.usage?.promptTokens)
        assertEquals(20, result.usage?.completionTokens)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/chat/completions"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `test chat with tool calls`() = runTest {
        val mockStreamResponse = """
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"role":"assistant","tool_calls":[{"index":0,"id":"call_123","type":"function","function":{"name":"get_weather","arguments":""}}]},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"{\"location\":"}}]},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"tool_calls":[{"index":0,"function":{"arguments":"\"Tokyo\"}"}}]},"finish_reason":"tool_calls"}],"usage":{"prompt_tokens":15,"completion_tokens":10,"total_tokens":25}}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "What's the weather in Tokyo?"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "gpt-4",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals(1, result.toolCalls?.size)
        assertEquals("call_123", result.toolCalls?.get(0)?.id)
        assertEquals("get_weather", result.toolCalls?.get(0)?.name)
        assertTrue(result.toolCalls?.get(0)?.arguments?.contains("Tokyo") == true)
    }
}
