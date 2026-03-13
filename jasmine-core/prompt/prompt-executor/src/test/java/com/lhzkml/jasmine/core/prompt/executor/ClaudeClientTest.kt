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

class ClaudeClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: ClaudeClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = ClaudeClient(
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
    fun `test messages API success`() = runTest {
        val mockResponse = """
            {
                "id": "msg_123",
                "type": "message",
                "role": "assistant",
                "content": [{
                    "type": "text",
                    "text": "Hello! How can I assist you?"
                }],
                "model": "claude-3-opus-20240229",
                "stop_reason": "end_turn",
                "usage": {
                    "input_tokens": 10,
                    "output_tokens": 20
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = ClaudeMessagesRequest(
            model = "claude-3-opus-20240229",
            messages = listOf(
                ClaudeMessage(role = "user", content = "Hello")
            ),
            maxTokens = 1024
        )

        val response = client.messages(request)

        assertNotNull(response)
        assertEquals("msg_123", response.id)
        assertEquals("assistant", response.role)
        assertEquals(1, response.content.size)
        assertEquals("Hello! How can I assist you?", response.content[0].text)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/messages"))
        assertEquals("test-api-key", recordedRequest.getHeader("x-api-key"))
        assertNotNull(recordedRequest.getHeader("anthropic-version"))
    }

    @Test
    fun `test streaming messages API`() = runTest {
        val mockStreamResponse = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_123","type":"message","role":"assistant","content":[],"model":"claude-3-opus-20240229"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"!"}}
            
            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":20}}
            
            event: message_stop
            data: {"type":"message_stop"}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val request = ClaudeMessagesRequest(
            model = "claude-3-opus-20240229",
            messages = listOf(ClaudeMessage(role = "user", content = "Hi")),
            maxTokens = 1024,
            stream = true
        )

        val events = mutableListOf<ClaudeStreamEvent>()
        client.streamMessages(request) { event ->
            events.add(event)
        }

        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it.type == "message_start" })
    }
}
