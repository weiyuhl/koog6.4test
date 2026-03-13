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

class ApiEmbeddingServiceTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var service: ApiEmbeddingService
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        val client = EmbeddingApiClient(
            apiKey = "test-api-key",
            baseUrl = mockServer.url("/").toString().trimEnd('/'),
            httpClient = OkHttpClient(),
            json = json
        )
        
        service = ApiEmbeddingService(
            client = client,
            model = "text-embedding-ada-002"
        )
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `test embed single text`() = runTest {
        val mockResponse = """
            {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "index": 0,
                        "embedding": [0.1, 0.2, 0.3, 0.4, 0.5]
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

        val embedding = service.embed("Hello world")

        assertNotNull(embedding)
        assertEquals(5, embedding.size)
        assertEquals(0.1f, embedding[0], 0.001f)
        assertEquals(0.5f, embedding[4], 0.001f)
    }

    @Test
    fun `test embed multiple texts`() = runTest {
        val mockResponse = """
            {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "index": 0,
                        "embedding": [0.1, 0.2, 0.3]
                    },
                    {
                        "object": "embedding",
                        "index": 1,
                        "embedding": [0.4, 0.5, 0.6]
                    },
                    {
                        "object": "embedding",
                        "index": 2,
                        "embedding": [0.7, 0.8, 0.9]
                    }
                ],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 15,
                    "total_tokens": 15
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val texts = listOf("Text 1", "Text 2", "Text 3")
        val embeddings = service.embedBatch(texts)

        assertNotNull(embeddings)
        assertEquals(3, embeddings.size)
        assertEquals(3, embeddings[0].size)
        assertEquals(0.1f, embeddings[0][0], 0.001f)
        assertEquals(0.4f, embeddings[1][0], 0.001f)
        assertEquals(0.7f, embeddings[2][0], 0.001f)
    }

    @Test
    fun `test embed empty text`() = runTest {
        val mockResponse = """
            {
                "object": "list",
                "data": [
                    {
                        "object": "embedding",
                        "index": 0,
                        "embedding": [0.0, 0.0, 0.0]
                    }
                ],
                "model": "text-embedding-ada-002",
                "usage": {
                    "prompt_tokens": 1,
                    "total_tokens": 1
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val embedding = service.embed("")

        assertNotNull(embedding)
        assertEquals(3, embedding.size)
        assertEquals(0.0f, embedding[0], 0.001f)
    }

    @Test
    fun `test dimension property`() {
        // 默认维度应该是 1536（OpenAI text-embedding-ada-002）
        assertEquals(1536, service.dimension)
    }
}
