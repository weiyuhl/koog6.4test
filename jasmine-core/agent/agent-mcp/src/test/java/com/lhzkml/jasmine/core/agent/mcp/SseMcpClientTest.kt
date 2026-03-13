package com.lhzkml.jasmine.core.agent.mcp

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SseMcpClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: SseMcpClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = SseMcpClient(
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
    fun `test SSE message parsing`() = runTest {
        val mockSseResponse = """
            event: message
            data: {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{},"serverInfo":{"name":"test-server","version":"1.0.0"}}}
            
            event: message
            data: {"jsonrpc":"2.0","method":"notifications/progress","params":{"progress":50,"total":100}}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockSseResponse)
            .addHeader("Content-Type", "text/event-stream")
            .addHeader("Cache-Control", "no-cache"))

        val request = McpInitializeRequest(
            protocolVersion = "2024-11-05",
            capabilities = McpClientCapabilities(),
            clientInfo = McpImplementationInfo(name = "test-client", version = "1.0.0")
        )

        val response = client.initialize(request)

        assertNotNull(response)
        assertEquals("2024-11-05", response.protocolVersion)
        assertEquals("test-server", response.serverInfo.name)
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("text/event-stream", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `test SSE streaming tool call`() = runTest {
        val mockSseResponse = """
            event: message
            data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"Processing..."}]}}
            
            event: progress
            data: {"progress":50,"total":100}
            
            event: message
            data: {"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"Complete!"}]}}
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockSseResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val request = McpCallToolRequest(
            name = "long_running_task",
            arguments = mapOf("duration" to 10)
        )

        val response = client.callTool(request)

        assertNotNull(response)
        assertTrue(response.content.isNotEmpty())
    }

    @Test
    fun `test SSE connection with keep-alive`() = runTest {
        val mockSseResponse = """
            : keep-alive comment
            
            event: message
            data: {"jsonrpc":"2.0","id":3,"result":{"tools":[]}}
            
            : another keep-alive
            
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockSseResponse)
            .addHeader("Content-Type", "text/event-stream"))

        val response = client.listTools()

        assertNotNull(response)
        assertEquals(0, response.tools.size)
    }
}
