package com.lhzkml.jasmine.core.prompt.executor

import com.lhzkml.jasmine.core.prompt.model.ChatMessage
import com.lhzkml.jasmine.core.prompt.model.SamplingParams
import kotlinx.coroutines.test.runTest
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

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = ClaudeClient(
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
    fun `test chat with streaming response`() = runTest {
        val mockStreamResponse = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_123","type":"message","role":"assistant","content":[],"model":"claude-3-opus-20240229","usage":{"input_tokens":10,"output_tokens":0}}}
            
            event: content_block_start
            data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
            
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

        val messages = listOf(ChatMessage(role = "user", content = "Hi"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "claude-3-opus-20240229",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals("Hello!", result.content)
        assertEquals(10, result.usage?.promptTokens)
        assertEquals(20, result.usage?.completionTokens)
        assertEquals("end_turn", result.finishReason)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/v1/messages"))
        assertEquals("test-api-key", recordedRequest.getHeader("x-api-key"))
        assertNotNull(recordedRequest.getHeader("anthropic-version"))
    }

    @Test
    fun `test chat with tool calls`() = runTest {
        val mockStreamResponse = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_123","usage":{"input_tokens":10,"output_tokens":0}}}
            
            event: content_block_start
            data: {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"tool_123","name":"get_weather"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\"location\":"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"\"Tokyo\"}"}}
            
            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"tool_use"},"usage":{"output_tokens":15}}
            
            event: message_stop
            data: {"type":"message_stop"}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "What's the weather in Tokyo?"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "claude-3-opus-20240229",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals(1, result.toolCalls?.size)
        assertEquals("tool_123", result.toolCalls?.get(0)?.id)
        assertEquals("get_weather", result.toolCalls?.get(0)?.name)
        assertTrue(result.toolCalls?.get(0)?.arguments?.contains("Tokyo") == true)
    }

    @Test
    fun `test list models`() = runTest {
        val mockResponse = """
            {
                "data": [
                    {
                        "id": "claude-3-opus-20240229",
                        "display_name": "Claude 3 Opus",
                        "type": "model"
                    },
                    {
                        "id": "claude-3-sonnet-20240229",
                        "display_name": "Claude 3 Sonnet",
                        "type": "model"
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
        assertEquals(2, models.size)
        assertEquals("claude-3-opus-20240229", models[0].id)
        assertEquals("Claude 3 Opus", models[0].displayName)
    }

    @Test
    fun `test chat with thinking`() = runTest {
        val mockStreamResponse = """
            event: message_start
            data: {"type":"message_start","message":{"id":"msg_123","usage":{"input_tokens":10,"output_tokens":0}}}
            
            event: content_block_start
            data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"Let me think..."}}
            
            event: content_block_start
            data: {"type":"content_block_start","index":1,"content_block":{"type":"text"}}
            
            event: content_block_delta
            data: {"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"The answer is 42"}}
            
            event: message_delta
            data: {"type":"message_delta","delta":{"stop_reason":"end_turn"},"usage":{"output_tokens":25}}
            
            event: message_stop
            data: {"type":"message_stop"}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val messages = listOf(ChatMessage(role = "user", content = "What is the meaning of life?"))
        val result = client.chatWithUsage(
            messages = messages,
            model = "claude-3-opus-20240229",
            maxTokens = 1024
        )

        assertNotNull(result)
        assertEquals("The answer is 42", result.content)
        assertEquals("Let me think...", result.thinking)
    }
}
