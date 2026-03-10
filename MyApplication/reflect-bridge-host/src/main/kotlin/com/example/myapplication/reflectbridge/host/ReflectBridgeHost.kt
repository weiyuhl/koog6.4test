package com.example.myapplication.reflectbridge.host

import com.example.myapplication.reflectbridge.ReflectBridgeExecuteRequest
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.util.concurrent.Executors

private val hostJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 8095
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.executor = Executors.newCachedThreadPool()
    server.createContext("/health") { exchange -> exchange.respond(200, "{\"status\":\"ok\"}") }
    server.createContext("/tools") { exchange ->
        if (exchange.requestMethod != "GET") exchange.respond(405, "Method not allowed")
        else exchange.respond(200, hostJson.encodeToString(ReflectBridgeService.snapshot()))
    }
    server.createContext("/execute") { exchange ->
        if (exchange.requestMethod != "POST") exchange.respond(405, "Method not allowed")
        else {
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val request = hostJson.decodeFromString<ReflectBridgeExecuteRequest>(body)
            val response = ReflectBridgeService.execute(request)
            exchange.respond(if (response.status == "success") 200 else 400, hostJson.encodeToString(response))
        }
    }
    server.start()
    println("Reflect bridge host listening on http://127.0.0.1:$port")
    println("Android 模拟器建议使用 http://10.0.2.2:$port")
    Thread.currentThread().join()
}

private fun HttpExchange.respond(status: Int, body: String) {
    responseHeaders.add("Content-Type", "application/json; charset=utf-8")
    val bytes = body.toByteArray(Charsets.UTF_8)
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}