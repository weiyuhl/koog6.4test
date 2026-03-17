package com.lhzkml.jasmine.core.assistant.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.assistant.memory.MemoryEntry
import com.lhzkml.jasmine.core.assistant.memory.MemoryStore
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.serialization.json.*

/**
 * 记忆管理工具集：允许助手自主进行多维记忆存储与对齐（Kai 深度对齐版）
 */
class MemoryTools(private val memoryStore: MemoryStore) {

    /**
     * 通用记忆存储
     */
    fun getStoreTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "memory_store",
            description = "Store or update a memory with a descriptive key. Use this proactively to remember user preferences, facts, and important information across conversations.",
            requiredParameters = listOf(
                ToolParameterDescriptor("key", "Descriptive key for the memory (e.g. user_name, preferred_language, project_details)", ToolParameterType.StringType),
                ToolParameterDescriptor("content", "The content to store", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val key = args["key"]?.jsonPrimitive?.content ?: return "Error: Missing key"
            val content = args["content"]?.jsonPrimitive?.content ?: return "Error: Missing content"
            
            memoryStore.store(key, content, category = MemoryEntry.CATEGORY_GENERAL, source = "tool-call")
            return "Success: Memory '$key' stored."
        }
    }

    /**
     * 结构化学习/更正存储
     */
    fun getLearnTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "memory_learn",
            description = "Store a structured learning with a category. Use LEARNING for things that worked, ERROR for error resolutions, PREFERENCE for user corrections/preferences.",
            requiredParameters = listOf(
                ToolParameterDescriptor("key", "Descriptive key for the learning", ToolParameterType.StringType),
                ToolParameterDescriptor("content", "What was learned or corrected", ToolParameterType.StringType),
                ToolParameterDescriptor("category", "Category: LEARNING, ERROR, or PREFERENCE", ToolParameterType.StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("source", "How this was learned: user_correction, observation, or error_resolution", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val key = args["key"]?.jsonPrimitive?.content ?: return "Error: Missing key"
            val content = args["content"]?.jsonPrimitive?.content ?: return "Error: Missing content"
            val catRaw = args["category"]?.jsonPrimitive?.content?.uppercase() ?: return "Error: Missing category"
            val source = args["source"]?.jsonPrimitive?.content ?: "observation"
            
            val category = when (catRaw) {
                "LEARNING" -> MemoryEntry.CATEGORY_LEARNING
                "ERROR" -> MemoryEntry.CATEGORY_ERROR
                "PREFERENCE" -> MemoryEntry.CATEGORY_PREFERENCE
                else -> return "Error: Invalid category. Use LEARNING, ERROR, or PREFERENCE."
            }
            
            memoryStore.store(key, content, category = category, source = source)
            return "Success: Learning '$key' stored in category '$category'."
        }
    }

    /**
     * 记忆强化
     */
    fun getReinforceTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "memory_reinforce",
            description = "Reinforce a stored memory by incrementing its hit count. Use this when a stored learning or preference produced a good outcome.",
            requiredParameters = listOf(
                ToolParameterDescriptor("key", "The exact key of the memory to reinforce", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val key = args["key"]?.jsonPrimitive?.content ?: return "Error: Missing key"
            
            val entry = memoryStore.getMemory(key) ?: return "Error: Memory not found: $key"
            memoryStore.store(key, entry.content, category = entry.category, source = "reinforcement")
            return "Success: Memory '$key' reinforced. Hit count increased."
        }
    }

    /**
     * 记忆遗忘
     */
    fun getForgetTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "memory_forget",
            description = "Delete a stored memory by its exact key.",
            requiredParameters = listOf(
                ToolParameterDescriptor("key", "The exact key of the memory to delete", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val key = args["key"]?.jsonPrimitive?.content ?: return "Error: Missing key"
            
            val removed = memoryStore.forget(key)
            return if (removed) "Success: Memory '$key' forgotten." else "Error: Memory not found."
        }
    }
}
