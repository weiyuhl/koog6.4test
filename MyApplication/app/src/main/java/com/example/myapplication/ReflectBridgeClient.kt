package com.example.myapplication

import com.example.myapplication.reflectbridge.BridgeFailureKind
import com.example.myapplication.reflectbridge.ReflectBridgeExecuteRequest
import com.example.myapplication.reflectbridge.ReflectBridgeExecuteResponse
import com.example.myapplication.reflectbridge.ReflectBridgeSnapshotDto
import com.example.myapplication.reflectbridge.ReflectBridgeToolDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Clock

class ReflectBridgeClient(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private data class HttpResponse(val statusCode: Int, val body: String)

    suspend fun fetchSnapshot(baseUrl: String): ReflectBridgeSnapshotDto =
        json.decodeFromString(request("GET", baseUrl = baseUrl, path = "/tools").body)

    suspend fun execute(
        baseUrl: String,
        tool: ReflectBridgeToolDto,
        rawInputs: Map<String, String>,
    ): ToolWorkbenchExecutionRecord {
        val timestamp = Clock.System.now().toString()
        val argsJson = runCatching {
            buildToolArgsJson(
                fields = tool.asWorkbenchDefinition(this, normalizeBaseUrl(baseUrl)).parameters,
                rawInputs = rawInputs,
            )
        }.getOrElse { error ->
            return ToolWorkbenchExecutionRecord(
                id = timestamp,
                toolName = tool.name,
                sourceLabel = tool.source,
                registrationLabel = tool.registration,
                status = "error",
                argsJson = "{}",
                resultText = "",
                failureKind = ToolWorkbenchFailureKind.ARGUMENT_PARSE_FAILURE,
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

        val httpResponse = runCatching {
            request(
                method = "POST",
                baseUrl = baseUrl,
                path = "/execute",
                requestBody = json.encodeToString(ReflectBridgeExecuteRequest(toolName = tool.name, argsJson = argsJson.toString())),
            )
        }.getOrElse { error ->
            return ToolWorkbenchExecutionRecord(
                id = timestamp,
                toolName = tool.name,
                sourceLabel = tool.source,
                registrationLabel = tool.registration,
                status = "error",
                argsJson = argsJson.toString(),
                resultText = "",
                failureKind = ToolWorkbenchFailureKind.TRANSPORT_FAILURE,
                errorText = error.message ?: error::class.simpleName ?: "Unknown error",
                timestamp = timestamp,
            )
        }

        val response = runCatching { json.decodeFromString<ReflectBridgeExecuteResponse>(httpResponse.body) }.getOrNull()
        if (response == null) {
            return ToolWorkbenchExecutionRecord(
                id = timestamp,
                toolName = tool.name,
                sourceLabel = tool.source,
                registrationLabel = tool.registration,
                status = "error",
                argsJson = argsJson.toString(),
                resultText = "",
                failureKind = ToolWorkbenchFailureKind.TRANSPORT_FAILURE,
                errorText = httpResponse.body.ifBlank { "Reflect bridge response decode failed" },
                timestamp = timestamp,
            )
        }

        return ToolWorkbenchExecutionRecord(
            id = timestamp,
            toolName = response.toolName,
            sourceLabel = response.source,
            registrationLabel = response.registration,
            status = response.status,
            argsJson = response.argsJson,
            resultText = response.resultText.orEmpty(),
            failureKind = response.errorKind?.toWorkbenchFailureKind()
                ?: if (httpResponse.statusCode in 200..299) null else ToolWorkbenchFailureKind.UNKNOWN,
            errorText = response.error,
            timestamp = timestamp,
        )
    }

    private suspend fun request(
        method: String,
        baseUrl: String,
        path: String,
        requestBody: String? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(normalizeBaseUrl(baseUrl) + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (requestBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
            }
        }

        val statusCode = connection.responseCode
        val body = try {
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        } finally {
            connection.disconnect()
        }
        HttpResponse(statusCode = statusCode, body = body)
    }

    private fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trim().trimEnd('/')
}