package com.lhzkml.jasmine.core.prompt.executor

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeepSeekClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: DeepSeekClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = DeepSeekClient(
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
    fun `test chat completion success`() = runTest {
        val mockResponse = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "deepseek-chat",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Hello! How can I help you?"
                    },
                    "finish_reason": "stop"
                }],
                "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 20,
                    "total_tokens": 30
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = ChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                ChatMessage(role = "user", content = "Hello")
            )
        )

        val response = client.chatCompletion(request)

        assertNotNull(response)
        assertEquals("chatcmpl-123", response.id)
        assertEquals("Hello! How can I help you?", response.choices[0].message.content)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/chat/completions"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `test streaming chat completion`() = runTest {
        val mockStreamResponse = """
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":null}]}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val request = ChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(ChatMessage(role = "user", content = "Hi")),
            stream = true
        )

        val chunks = mutableListOf<ChatCompletionChunk>()
        client.streamChatCompletion(request) { chunk ->
            chunks.add(chunk)
        }

        assertTrue(chunks.size >= 2)
    }
}
