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

class DeepSeekClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: DeepSeekClient

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = DeepSeekClient(
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
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-chat","choices":[{"index":0,"delta":{"role":"assistant","content":"Hello"},"finish_reason":null}]}
            
            data: {"id":"chatcmpl-123","object":"chat.completion.chunk","created":1677652288,"model":"deepseek-chat","choices":[{"index":0,"delta":{"content":"!"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":20,"total_tokens":30}}
            
            data: [DONE]
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "Hello"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "deepseek-chat",
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
    fun `test get balance`() = runTest {
        val mockResponse = """
            {
                "is_available": true,
                "balance_infos": [
                    {
                        "currency": "CNY",
                        "total_balance": "100.00",
                        "granted_balance": "20.00",
                        "topped_up_balance": "80.00"
                    }
                ]
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
        assertEquals("100.00", balance.balances[0].totalBalance)
        assertEquals("20.00", balance.balances[0].grantedBalance)
        assertEquals("80.00", balance.balances[0].toppedUpBalance)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/user/balance"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }
}
