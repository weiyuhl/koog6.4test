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

class HttpMcpClientTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: HttpMcpClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        client = HttpMcpClient(
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
    fun `test initialize success`() = runTest {
        val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "result": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {
                        "tools": {}
                    },
                    "serverInfo": {
                        "name": "test-server",
                        "version": "1.0.0"
                    }
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

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
        assertTrue(recordedRequest.path!!.contains("/mcp/v1"))
        assertEquals("application/json", recordedRequest.getHeader("Content-Type"))
    }

    @Test
    fun `test list tools success`() = runTest {
        val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": 2,
                "result": {
                    "tools": [
                        {
                            "name": "get_weather",
                            "description": "Get weather information",
                            "inputSchema": {
                                "type": "object",
                                "properties": {
                                    "location": {
                                        "type": "string",
                                        "description": "City name"
                                    }
                                },
                                "required": ["location"]
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val response = client.listTools()

        assertNotNull(response)
        assertEquals(1, response.tools.size)
        assertEquals("get_weather", response.tools[0].name)
        assertEquals("Get weather information", response.tools[0].description)
    }

    @Test
    fun `test call tool success`() = runTest {
        val mockResponse = """
            {
                "jsonrpc": "2.0",
                "id": 3,
                "result": {
                    "content": [
                        {
                            "type": "text",
                            "text": "The weather in Tokyo is sunny, 25°C"
                        }
                    ]
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockResponse)
            .addHeader("Content-Type", "application/json"))

        val request = McpCallToolRequest(
            name = "get_weather",
            arguments = mapOf("location" to "Tokyo")
        )

        val response = client.callTool(request)

        assertNotNull(response)
        assertEquals(1, response.content.size)
        assertTrue(response.content[0].text.contains("Tokyo"))
        assertTrue(response.content[0].text.contains("sunny"))
    }

    @Test
    fun `test error response handling`() = runTest {
        val mockErrorResponse = """
            {
                "jsonrpc": "2.0",
                "id": 4,
                "error": {
                    "code": -32601,
                    "message": "Method not found"
                }
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockErrorResponse)
            .addHeader("Content-Type", "application/json"))

        try {
            client.listTools()
            fail("Expected exception for error response")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Method not found") == true)
        }
    }
}
