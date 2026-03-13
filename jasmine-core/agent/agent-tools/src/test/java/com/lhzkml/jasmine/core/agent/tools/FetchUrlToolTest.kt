package com.lhzkml.jasmine.core.agent.tools

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FetchUrlToolTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var tool: FetchUrlTool

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        
        tool = FetchUrlTool(
            httpClient = OkHttpClient()
        )
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `test fetch HTML content`() = runTest {
        val mockHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Test Page</title></head>
            <body>
                <h1>Welcome</h1>
                <p>This is a test page with some content.</p>
            </body>
            </html>
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html"))

        val url = mockServer.url("/test").toString()
        val result = tool.fetch(url)

        assertNotNull(result)
        assertTrue(result.contains("Welcome"))
        assertTrue(result.contains("test page"))
        
        val recordedRequest = mockServer.takeRequest()
        assertEquals("GET", recordedRequest.method)
        assertEquals("/test", recordedRequest.path)
    }

    @Test
    fun `test fetch JSON content`() = runTest {
        val mockJson = """
            {
                "title": "Test Article",
                "content": "This is test content",
                "author": "Test Author"
            }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockJson)
            .addHeader("Content-Type", "application/json"))

        val url = mockServer.url("/api/article").toString()
        val result = tool.fetch(url)

        assertNotNull(result)
        assertTrue(result.contains("Test Article"))
        assertTrue(result.contains("test content"))
    }

    @Test
    fun `test fetch with redirect`() = runTest {
        mockServer.enqueue(MockResponse()
            .setResponseCode(301)
            .addHeader("Location", "/redirected"))
        
        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody("<html><body>Redirected content</body></html>")
            .addHeader("Content-Type", "text/html"))

        val url = mockServer.url("/original").toString()
        val result = tool.fetch(url)

        assertNotNull(result)
        assertTrue(result.contains("Redirected content"))
    }

    @Test
    fun `test fetch error handling`() = runTest {
        mockServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))

        val url = mockServer.url("/notfound").toString()
        
        try {
            tool.fetch(url)
            fail("Expected exception for 404 error")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("404") == true)
        }
    }

    @Test
    fun `test fetch with large content`() = runTest {
        val largeContent = "x".repeat(10000)
        val mockHtml = """
            <html>
            <body>
                <p>$largeContent</p>
            </body>
            </html>
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(mockHtml)
            .addHeader("Content-Type", "text/html"))

        val url = mockServer.url("/large").toString()
        val result = tool.fetch(url)

        assertNotNull(result)
        assertTrue(result.length > 1000)
    }
}
