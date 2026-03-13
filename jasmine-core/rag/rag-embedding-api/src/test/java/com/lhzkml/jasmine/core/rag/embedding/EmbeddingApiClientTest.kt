package com.lhzkml.jasmine.core.rag.embedding

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EmbeddingApiClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: EmbeddingApiClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = EmbeddingApiClient(
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
    fun `test create embeddings success`() = runTest {
        val mockResponse = """
            {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "index": 0,
                        "embedding": [0.1, 0.2, 0.3, 0.4, 0.5]
                    },
                    {
                        "object": "embedding",
                        "index": 1,
                        "embedding": [0.6, 0.7, 0.8, 0.9, 1.0]
                    }
                ],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 10,
                    "total_tokens": 10
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = EmbeddingRequest(
            model = "text-embedding-ada-002",
            input = listOf("Hello world", "Test text")
        )

        val response = client.createEmbeddings(request)

        assertNotNull(response)
        assertEquals(2, response.data.size)
        assertEquals(5, response.data[0].embedding.size)
        assertEquals(0.1f, response.data[0].embedding[0], 0.001f)
        assertEquals(0.6f, response.data[1].embedding[0], 0.001f)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.path!!.contains("/embeddings"))
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `test create embeddings with single input`() = runTest {
        val mockResponse = """
            {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "index": 0,
                        "embedding": [0.1, 0.2, 0.3]
                    }
                ],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 5,
                    "total_tokens": 5
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = EmbeddingRequest(
            model = "text-embedding-ada-002",
            input = listOf("Single text")
        )

        val response = client.createEmbeddings(request)

        assertNotNull(response)
        assertEquals(1, response.data.size)
        assertEquals(3, response.data[0].embedding.size)
    }

    @Test
    fun `test error handling`() = runTest {
        val mockErrorResponse = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "invalid_request_error",
                    "code": "invalid_api_key"
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody(mockErrorResponse)
            .addHeader("Content-Type", "application/json"))

        val request = EmbeddingRequest(
            model = "text-embedding-ada-002",
            input = listOf("Test")
        )

        try {
            client.createEmbeddings(request)
            fail("Expected exception for 401 error")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("401") == true)
        }
    }
}
