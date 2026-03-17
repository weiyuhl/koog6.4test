package com.lhzkml.jasmine.core.assistant.runtime

import com.lhzkml.jasmine.core.assistant.memory.MemoryStore
import com.lhzkml.jasmine.core.assistant.scheduler.TaskStore
import com.lhzkml.jasmine.core.assistant.email.EmailStore
import java.util.Calendar

/**
 * 助手心跳管理器
 * 实现“自省”逻辑，判断何时需要主动触发 AI 审查状态。
 */
class HeartbeatManager(
    private val memoryStore: MemoryStore,
    private val taskStore: TaskStore,
    private val emailStore: EmailStore? = null
) {
    private var lastHeartbeatEpochMs: Long = 0

    /**
     * 检查是否需要执行心跳自检
     */
    fun isHeartbeatDue(intervalMinutes: Int = 30): Boolean {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // 允许工作时段：8:00 - 22:00
        if (currentHour < 8 || currentHour >= 22) return false
        
        return (now - lastHeartbeatEpochMs) >= (intervalMinutes * 60_000L)
    }

    /**
     * 生成心跳自省提示词（对齐 Kai 原生模板）
     */
    fun buildHeartbeatPrompt(): String {
        return buildString {
            append("[HEARTBEAT] This is an automatic self-check. Review your memories and pending tasks. ")
            append("If everything looks good and nothing needs attention, respond with exactly: HEARTBEAT_OK\n")
            append("If something needs attention (stale memories, due tasks, user follow-ups), address it.")
            
            // 注入待办任务摘要
            val pendingTasks = taskStore.getPendingTasks()
            if (pendingTasks.isNotEmpty()) {
                append("\n\n## Pending Tasks for Review\n")
                pendingTasks.forEach { append("- ${it.description} (due soon)\n") }
            }
            
            // 注入强化学习晋升建议
            val candidates = memoryStore.getPromotionCandidates()
            if (candidates.isNotEmpty()) {
                append("\n\n## Promotion Candidates\n")
                append("These patterns have been reinforced multiple times. Consider if they should be promoted to permanent soul rules:\n")
                candidates.forEach { append("- **${it.content}** (Reinforced ${it.hitCount}x)\n") }
            }

            // 注入邮件状态
            val store = emailStore
            if (store != null) {
                val accounts = store.getAccounts()
                if (accounts.isNotEmpty()) {
                    append("\n\n## Email Status\n")
                    accounts.forEach { acc ->
                        val state = store.getSyncState(acc.id)
                        append("- **${acc.email}**: ${state.unreadCount} unread emails\n")
                    }
                }
            }
        }
    }

    fun recordHeartbeat() {
        lastHeartbeatEpochMs = System.currentTimeMillis()
    }
}
