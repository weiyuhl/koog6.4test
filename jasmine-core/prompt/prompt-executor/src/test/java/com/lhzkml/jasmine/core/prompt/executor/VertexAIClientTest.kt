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

class VertexAIClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: VertexAIClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = VertexAIClient(
            projectId = "test-project",
            location = "us-central1",
            accessToken = "test-token",
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
    fun `test generate content success`() = runTest {
        val mockResponse = """
            {
                "candidates": [{
                    "content": {
                        "parts": [{
                            "text": "Hello from Vertex AI!"
                        }],
                        "role": "model"
                    },
                    "finishReason": "STOP"
                }],
                "usageMetadata": {
                    "promptTokenCount": 10,
                    "candidatesTokenCount": 20,
                    "totalTokenCount": 30
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = "Hello")),
                    role = "user"
                )
            )
        )

        val response = client.generateContent("gemini-pro", request)

        assertNotNull(response)
        assertEquals(1, response.candidates.size)
        assertEquals("Hello from Vertex AI!", response.candidates[0].content.parts[0].text)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("test-project"))
        assertTrue(recordedRequest.path!!.contains("us-central1"))
        assertTrue(recordedRequest.path!!.contains("gemini-pro:generateContent"))
        assertEquals("Bearer test-token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `test streaming generate content`() = runTest {
        val mockStreamResponse = """
            data: {"candidates":[{"content":{"parts":[{"text":"Hello"}],"role":"model"}}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":" from Vertex"}],"role":"model"},"finishReason":"STOP"}]}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockStreamResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val request = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = "Hi")),
                    role = "user"
                )
            )
        )

        val responses = mutableListOf<GeminiGenerateContentResponse>()
        client.streamGenerateContent("gemini-pro", request) { response ->
            responses.add(response)
        }

        assertTrue(responses.size >= 2)
    }
}
