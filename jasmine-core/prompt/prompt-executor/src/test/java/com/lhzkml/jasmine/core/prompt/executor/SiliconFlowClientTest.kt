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

class SiliconFlowClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: SiliconFlowClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = SiliconFlowClient(
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
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"Qwen/Qwen2.5-7B-Instruct","choices":[{"index":0,"delta":{"role":"assistant","content":"你好"},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"Qwen/Qwen2.5-7B-Instruct","choices":[{"index":0,"delta":{"content":"！"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "你好"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "Qwen/Qwen2.5-7B-Instruct",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals("你好！", result.content)
        assertEquals(10, result.usage?.promptTokens)
        assertEquals(20, result.usage?.completionTokens)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/chat/completions"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
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
