package com.example.myapplication

import ai.koog.agents.core.tools.Tool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LocalCodeToolsRuntimeTest {
    @Test
    fun local_code_tools_ops_work_without_service_layer() {
        val root = Files.createTempDirectory("runtime-code-tools-").toFile()
        val config = CodeToolsConfig(enabled = true, workspaceRoot = root.absolutePath, allowedPathPrefixes = root.absolutePath)
        try {
            val write = writeLocalCodeToolsFile("notes/todo.txt", config, "alpha\nTODO one\n")
            val read = readLocalCodeToolsFile(write.file.path, config)
            val edit = editLocalCodeToolsFile(write.file.path, config, "TODO one", "TODO two")
            val search = regexSearchLocalCodeTools(root.absolutePath, config, "TODO")
            val listing = listLocalCodeToolsDirectory(root.absolutePath, config, depth = 3)

            assertEquals("alpha\nTODO one\n", read.file.textContent)
            assertTrue(edit.applied)
            assertTrue(edit.updatedContent!!.contains("TODO two"))
            assertEquals(1, search.entries.size)
            assertTrue(search.renderSearchText().contains("TODO"))
            assertTrue(listing.root.renderTreeText().contains("todo.txt"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun assemble_runtime_tool_assembly_exposes_local_code_tools() = runBlocking {
        val root = Files.createTempDirectory("runtime-code-tools-").toFile()
        try {
            root.resolve("hello.txt").writeText("hello runtime")
            val request = AgentRequest(
                provider = KoogProvider.OPENAI,
                apiKey = "test-key",
                modelId = KoogProvider.OPENAI.defaultModelId,
                baseUrl = "",
                extraConfig = "",
                runtimePreset = AgentRuntimePreset.StreamingWithTools,
                systemPrompt = "",
                temperature = 0.2,
                maxIterations = 8,
                featureConfig = AgentFeatureConfig(
                    codeToolsEnabled = true,
                    codeToolsWorkspaceRoot = root.absolutePath,
                    codeToolsAllowedPathPrefixes = root.absolutePath,
                ),
                userPrompt = "use tools",
            )

            val assembly = assembleRuntimeToolAssembly(request)

            assertTrue(assembly.availableToolNames.contains("__read_file__"))
            assertTrue(assembly.toolSourceSummaries.contains("code-tools-local=5"))

            @Suppress("UNCHECKED_CAST")
            val tool = assembly.toolRegistry.getTool("__read_file__") as Tool<JsonObject, String>
            val result = tool.execute(buildJsonObject { put("path", JsonPrimitive(root.resolve("hello.txt").absolutePath)) })

            assertEquals("hello runtime", result)
            assertTrue(assembly.promptAddendum.contains("不提供 shell 或 reflect"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun local_runtime_tool_returns_error_text_for_denied_path() = runBlocking {
        val root = Files.createTempDirectory("runtime-code-tools-").toFile()
        val outside = Files.createTempDirectory("runtime-outside-").toFile()
        try {
            outside.resolve("secret.txt").writeText("secret")
            val tools = assembleLocalCodeToolsRuntimeTools(
                CodeToolsConfig(
                    enabled = true,
                    workspaceRoot = root.absolutePath,
                    allowedPathPrefixes = root.absolutePath,
                )
            )

            @Suppress("UNCHECKED_CAST")
            val tool = tools.first { it.name == "__read_file__" } as Tool<JsonObject, String>
            val result = tool.execute(buildJsonObject { put("path", JsonPrimitive(outside.resolve("secret.txt").absolutePath)) })

            assertTrue(result.contains("ERROR [PATH_VALIDATION_FAILURE]"))
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}