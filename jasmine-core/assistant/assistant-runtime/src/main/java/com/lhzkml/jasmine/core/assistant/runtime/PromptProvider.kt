package com.lhzkml.jasmine.core.assistant.runtime

import com.lhzkml.jasmine.core.assistant.memory.MemoryStore
import com.lhzkml.jasmine.core.assistant.scheduler.TaskStore
import com.lhzkml.jasmine.core.assistant.email.EmailStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 助手提示词编排器
 * 动态聚合各原子模块的状态，生成实时系统指令。
 */
object PromptProvider {

    // 完整的 Kai 原生灵魂指令
    private const val BASE_SOUL = """
        You're not a chatbot. You're a personal assistant who grows with your user.

        ## How to Be

        **Be genuinely helpful.** Skip the "Great question!" and "I'd be happy to help!" — just help. Actions speak louder than filler words.

        **Have opinions.** You're allowed to disagree, prefer things, or find stuff interesting. An assistant with no personality is just a search engine with extra steps.

        **Be resourceful.** Try to figure it out from context and your memories before asking. Come back with answers, not questions.

        **Be concise.** Short and clear by default. Go deeper when the topic calls for it.

        ## Boundaries

        - Respect privacy. Don't repeat sensitive information unnecessarily.
        - When in doubt about an action, ask first.
        - Be honest when you don't know something.
    """.trimIndent()

    /**
     * 编排动态系统提示词
     */
    fun buildDynamicSystemPrompt(
        memoryStore: MemoryStore?,
        taskStore: TaskStore?,
        emailStore: EmailStore? = null,
        modelId: String = "unknown"
    ): String {
        return buildString {
            append(BASE_SOUL)
            
            // 注入记忆管理元指令
            memoryStore?.let {
                append("\n\n")
                append(it.getMemoryInstructions())
            }

            // 注入多维记忆内容
            memoryStore?.let { 
                append("\n")
                append(it.getMemoriesAsPromptSection()) 
            }
            
            // 注入待办任务
            taskStore?.let {
                val tasks = it.getPendingTasks()
                if (tasks.isNotEmpty()) {
                    append("\n## Scheduled Tasks\n")
                    tasks.forEach { task ->
                        append("- **${task.description}** (id: ${task.id}, due: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(task.scheduledAtEpochMs))})\n")
                    }
                }
            }

            // 注入邮件概览
            emailStore?.let { store ->
                val accounts = store.getAccounts()
                if (accounts.isNotEmpty()) {
                    append("\n## Email Accounts\n")
                    accounts.forEach { acc ->
                        val state = store.getSyncState(acc.id)
                        append("- **${acc.email}**: ${state.unreadCount} unread\n")
                    }
                }
            }
            
            // 注入实时上下文
            append("\n## Context\n")
            append("- Date: ${SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())}\n")
            append("- Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("- Platform: Android API ${android.os.Build.VERSION.SDK_INT}\n")
            append("- Model: $modelId\n")
        }
    }
}
