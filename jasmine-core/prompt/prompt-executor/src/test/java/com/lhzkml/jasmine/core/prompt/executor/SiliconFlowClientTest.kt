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

class SiliconFlowClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: SiliconFlowClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = SiliconFlowClient(
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
                "model": "Qwen/Qwen2.5-7B-Instruct",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "你好！我能帮你什么？"
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
            model = "Qwen/Qwen2.5-7B-Instruct",
            messages = listOf(
                ChatMessage(role = "user", content = "你好")
            )
        )

        val response = client.chatCompletion(request)

        assertNotNull(response)
        assertEquals("chatcmpl-123", response.id)
        assertEquals("你好！我能帮你什么？", response.choices[0].message.content)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/chat/completions"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `test streaming chat completion`() = runTest {
        val mockStreamResponse = """
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"role":"assistant","content":"你好"},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","choices":[{"index":0,"delta":{"content":"！"},"finish_reason":null}]}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val request = ChatCompletionRequest(
            model = "Qwen/Qwen2.5-7B-Instruct",
            messages = listOf(ChatMessage(role = "user", content = "Hi")),
            stream = true
        )

        val chunks = mutableListOf<ChatCompletionChunk>()
        client.streamChatCompletion(request) { chunk ->
            chunks.add(chunk)
        }

        assertTrue(chunks.size >= 2)
    }

    @Test
    fun `test get balance`() = runTest {
        val mockResponse = """
            {
                "code": 0,
                "message": "success",
                "status": true,
                "data": {
                    "balance": "0.88",
                    "chargeBalance": "88.00",
                    "totalBalance": "88.88"
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val balance = client.getBalance()

        assertNotNull(balance)
        assertTrue(balance.isAvailable)
        assertEquals(1, balance.balances.size)
        assertEquals("CNY", balance.balances[0].currency)
        assertEquals("88.88", balance.balances[0].totalBalance)
        assertEquals("0.88", balance.balances[0].grantedBalance)
        assertEquals("88.00", balance.balances[0].toppedUpBalance)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/user/info"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }
}
