package com.lhzkml.jasmine.core.assistant.tools

import com.lhzkml.jasmine.core.agent.tools.Tool
import com.lhzkml.jasmine.core.assistant.scheduler.TaskStatus
import com.lhzkml.jasmine.core.assistant.scheduler.TaskStore
import com.lhzkml.jasmine.core.prompt.model.ToolDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterDescriptor
import com.lhzkml.jasmine.core.prompt.model.ToolParameterType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.*

/**
 * 调度管理工具：允许助手管理延时与周期性任务（Kai 深度对齐版）
 */
class SchedulingTools(private val taskStore: TaskStore) {

    /**
     * 创建调度任务
     */
    fun getScheduleTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "schedule_task",
            description = "Schedule a task for future or recurring execution. Use this for reminders, delayed sends, or recurring jobs. When scheduling a reminder, write the prompt as the actual reminder text the user will see. Include recent context if appropriate. At least one of execute_at or cron must be provided.",
            requiredParameters = listOf(
                ToolParameterDescriptor("description", "Human-readable description of the task", ToolParameterType.StringType),
                ToolParameterDescriptor("prompt", "The prompt to send to the AI when the task fires", ToolParameterType.StringType)
            ),
            optionalParameters = listOf(
                ToolParameterDescriptor("execute_at", "ISO 8601 datetime string (e.g. 2025-03-15T09:00:00)", ToolParameterType.StringType),
                ToolParameterDescriptor("cron", "Cron expression (e.g. '0 9 * * 1' for every Monday at 9am)", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val description = args["description"]?.jsonPrimitive?.content ?: return "Error: Missing description"
            val prompt = args["prompt"]?.jsonPrimitive?.content ?: return "Error: Missing prompt"
            val executeAt = args["execute_at"]?.jsonPrimitive?.content
            val cron = args["cron"]?.jsonPrimitive?.content

            if (executeAt == null && cron == null) {
                return "Error: At least one of execute_at or cron must be provided"
            }

            val scheduledAtEpochMs = if (executeAt != null) {
                try {
                    parseIso8601ToEpochMs(executeAt)
                } catch (e: Exception) {
                    return "Error: Invalid execute_at format: ${e.message}"
                }
            } else {
                0L
            }

            val task = taskStore.addTask(
                description = description,
                prompt = prompt,
                scheduledAtEpochMs = scheduledAtEpochMs,
                cron = cron
            )

            return """
                {
                    "success": true,
                    "task_id": "${task.id}",
                    "description": "${task.description}",
                    "scheduled_at": "${executeAt ?: "recurring"}",
                    "cron": "${cron ?: "none"}"
                }
            """.trimIndent()
        }
    }

    /**
     * 取消调度任务
     */
    fun getCancelTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "cancel_task",
            description = "Cancel a scheduled task by its ID. When the user asks to stop, cancel, or remove any scheduled or recurring task, call this tool with the matching task ID. If unsure, call list_tasks first.",
            requiredParameters = listOf(
                ToolParameterDescriptor("task_id", "The ID of the task to cancel", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val taskId = args["task_id"]?.jsonPrimitive?.content ?: return "Error: Missing task_id"

            val removed = taskStore.removeTask(taskId)
            return if (removed) {
                "Success: Task $taskId canceled."
            } else {
                "Error: Task not found: $taskId"
            }
        }
    }

    /**
     * 列出所有任务
     */
    fun getListTool() = object : Tool() {
        override val descriptor = ToolDescriptor(
            name = "list_tasks",
            description = "List all scheduled tasks with their IDs, descriptions, and status. Call this before cancel_task if you need to find a task ID.",
            optionalParameters = listOf(
                ToolParameterDescriptor("status", "Filter by status: PENDING or COMPLETED", ToolParameterType.StringType)
            )
        )

        override suspend fun execute(arguments: String): String {
            val args = Json.parseToJsonElement(arguments).jsonObject
            val statusFilter = args["status"]?.jsonPrimitive?.content?.uppercase()
            val allTasks = taskStore.getAllTasks()

            val filtered = if (statusFilter != null) {
                val status = try {
                    TaskStatus.valueOf(statusFilter)
                } catch (e: Exception) {
                    return "Error: Invalid status: $statusFilter. Use PENDING or COMPLETED"
                }
                allTasks.filter { it.status == status }
            } else {
                allTasks
            }

            if (filtered.isEmpty()) return "No tasks found."

            return filtered.joinToString("\n") { task ->
                "- [${task.status}] ID: ${task.id} | Desc: ${task.description} | Cron: ${task.cron ?: "none"} | Next: ${task.scheduledAtEpochMs}"
            }
        }
    }

    private fun parseIso8601ToEpochMs(isoString: String): Long {
        return try {
            Instant.parse(isoString).toEpochMilliseconds()
        } catch (e: Exception) {
            val localDateTime = LocalDateTime.parse(isoString)
            localDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        }
    }
}
